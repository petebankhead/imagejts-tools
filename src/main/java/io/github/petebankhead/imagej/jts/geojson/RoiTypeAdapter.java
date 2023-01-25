package io.github.petebankhead.imagej.jts.geojson;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import ij.gui.Roi;
import io.github.petebankhead.imagej.jts.converters.RoiToGeometryConverter;

public class RoiTypeAdapter extends TypeAdapter<Roi> {
	
	private NumberFormat nf;
	private boolean asFeature = true;
	private GeometryFactory factory = new GeometryFactory();
	
	public RoiTypeAdapter() {
		this(3);
	}
	
	public RoiTypeAdapter(int numDecimalPlaces) {
		nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(numDecimalPlaces);
	}
	

	@Override
	public void write(JsonWriter out, Roi roi) throws IOException {
		
		out.beginObject();
		
		if (roi != null) {

			Geometry geometry = new RoiToGeometryConverter().roiToGeometry(roi);

			if (asFeature) {
				out.name("type");
				out.value("feature");
				out.name("geometry");
				out.beginObject();
				writeGeometry(geometry, out);
				out.endObject();
				out.name("properties");
				out.beginObject();
				if (roi.getCPosition() > 0) {
					out.name("c");
					out.value(roi.getCPosition());
				}
				if (roi.getZPosition() > 0) {
					out.name("z");
					out.value(roi.getZPosition());
				}
				if (roi.getTPosition() > 0) {
					out.name("t");
					out.value(roi.getTPosition());
				}
				if (roi.getPosition() > 0) {
					out.name("position");
					out.value(roi.getTPosition());					
				}
				if (roi.getName() != null) {
					out.name("name");
					out.value(roi.getName());										
				}
				String group = Roi.getGroupName(roi.getGroup());
				if (group != null) {
					out.name("group");
					out.value(group);															
				}
				Color strokeColor = roi.getStrokeColor();
				if (strokeColor != null) {
					writeColor(out, "strokeColor", strokeColor);
				}
				Color fillColor = roi.getFillColor();
				if (fillColor != null) {
					writeColor(out, "fillColor", fillColor);
				}
				out.endObject();
			} else {
				writeGeometry(geometry, out);
			}
			
			// TODO: Consider adding flag for ellipse/oval ROIs
			
		}
					
		out.endObject();
	}
	
	
	private static void writeColor(JsonWriter out, String name, Color color) throws IOException {
		out.name(name);
		out.beginArray();
		out.value(color.getRed());
		out.value(color.getGreen());
		out.value(color.getBlue());
		out.endArray();
	}
	

	@Override
	public Roi read(JsonReader in) throws IOException {
		
		JsonObject obj = (JsonObject)TypeAdapters.JSON_ELEMENT.read(in);
		JsonObject geomObj;
		JsonObject propertiesObj;
		if ("feature".equals(obj.get("type"))) {
			propertiesObj = obj.has("properties") ? obj.get("properties").getAsJsonObject() : null;
			geomObj = propertiesObj.get("geometry").getAsJsonObject();
		} else {
			propertiesObj = null;
			geomObj = obj;
		}
		
		
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * Write a Geometry as GeoJSON. Note that this does <i>not</i> call beginObject() and endObject() 
	 * so as to provide an opportunity to add additional fields. Rather it only writes the key 
	 * type, coordinates and (for GeometryCollections) geometries fields.
	 * @param geometry
	 * @param out
	 * @throws IOException
	 */
	private void writeGeometry(Geometry geometry, JsonWriter out) throws IOException {
		String type = geometry.getGeometryType();
		out.name("type");
		out.value(type);

		if ("GeometryCollection".equals(geometry.getGeometryType())) {
			out.beginArray();
			for (int i = 0; i < geometry.getNumGeometries(); i++) {
				writeGeometry(geometry.getGeometryN(i), out);
			}
			out.endArray();
		} else {
			out.name("coordinates");
			writeCoordinates(geometry, out);			
		}
	}


	private void writeCoordinates(Geometry geometry, JsonWriter out) throws IOException {
		if (geometry instanceof Point)
			writeCoordinates((Point)geometry, out);
		else if (geometry instanceof MultiPoint)
			writeCoordinates((MultiPoint)geometry, out);
		else if (geometry instanceof LineString)
			writeCoordinates((LineString)geometry, out);
		else if (geometry instanceof MultiLineString)
			writeCoordinates((MultiLineString)geometry, out);
		else if (geometry instanceof Polygon)
			writeCoordinates((Polygon)geometry, out);
		else if (geometry instanceof MultiPolygon)
			writeCoordinates((MultiPolygon)geometry, out);
		else
			throw new IllegalArgumentException("Unable to write coordinates for geometry type " + geometry.getGeometryType());
	}

	private void writeCoordinates(Point point, JsonWriter out) throws IOException {
		out.jsonValue(coordinateToString(point.getCoordinate()));
	}

	private void writeCoordinates(MultiPoint multiPoint, JsonWriter out) throws IOException {
		Coordinate[] coords = multiPoint.getCoordinates();
		out.beginArray();
		for (Coordinate c : coords)
			out.jsonValue(coordinateToString(c));
		out.endArray();
	}

	private void writeCoordinates(LineString lineString, JsonWriter out) throws IOException {
		Coordinate[] coords = lineString.getCoordinates();
		out.beginArray();
		for (Coordinate c : coords)
			out.jsonValue(coordinateToString(c));
		out.endArray();
	}

	private void writeCoordinates(Polygon polygon, JsonWriter out) throws IOException {
		out.beginArray();
		writeCoordinates(polygon.getExteriorRing(), out);
		for (int i = 0; i < polygon.getNumInteriorRing(); i++)
			writeCoordinates(polygon.getInteriorRingN(i), out);
		out.endArray();
	}

	private void writeCoordinates(MultiPolygon multiPolygon, JsonWriter out) throws IOException {
		out.beginArray();
		for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
			writeCoordinates(multiPolygon.getGeometryN(i), out);
		out.endArray();
	}


	private void writeMultiPoint(MultiPoint multiPoint, JsonWriter out) throws IOException {
		out.name("type");
		out.value("MultiPoint");
		
		out.name("coordinates");
		out.beginArray();
		Coordinate[] coords = multiPoint.getCoordinates();
		for (Coordinate c : coords)
			out.jsonValue(coordinateToString(c));
		out.endArray();
	}

	private void writeLineString(LineString lineString, JsonWriter out) throws IOException {
		out.name("type");
		out.value("LineString");
		
		out.name("coordinates");
		out.beginArray();
		CoordinateSequence coords = lineString.getCoordinateSequence();
		for (int i = 0; i < coords.size(); i++)
			out.jsonValue(coordinateToString(coords.getX(i), coords.getY(i)));
		out.endArray();
	}

	private String coordinateToString(Coordinate coord) {
		return coordinateToString(coord.x, coord.y);
	}

	private String coordinateToString(double x, double y) {
		return "[" + nf.format(x) + ", "
				+ nf.format(y) + "]";		
	}
	
	
	
	
	private Geometry parseGeometry(JsonObject obj, GeometryFactory factory) {
		if (!obj.has("type"))
			return null;
		
		String type = obj.get("type").getAsString();
		JsonArray coordinates = null;
		if (obj.has("coordinates"))
			coordinates = obj.getAsJsonArray("coordinates").getAsJsonArray();
			
		switch (type) {
		case "Point":
			return parsePoint(coordinates, factory);
		case "MultiPoint":
			return parseMultiPoint(coordinates, factory);
		case "LineString":
			return parseLineString(coordinates, factory);
		case "MultiLineString":
			return parseMultiLineString(coordinates, factory);
		case "Polygon":
			return parsePolygon(coordinates, factory);
		case "MultiPolygon":
			return parseMultiPolygon(coordinates, factory);
		case "GeometryCollection":
			return parseGeometryCollection(obj, factory);
		}
		throw new IllegalArgumentException("No Geometry type found for object " + obj);
	}

	/**
	 * Parse a coordinate from a JsonArray. No error checking is performed. Supports either two elements (x,y) or three (x,y,z).
	 * @param array
	 * @return
	 */
	private Coordinate parseCoordinate(JsonArray array) {
		double x = array.get(0).getAsDouble();
		double y = array.get(1).getAsDouble();
		if (array.size() == 2)
			return new Coordinate(x, y);
		else {
			double z = array.get(2).getAsDouble();
			return new Coordinate(x, y, z);
		}
	}

	private Coordinate[] parseCoordinateArray(JsonArray array) {
		Coordinate[] coordinates = new Coordinate[array.size()];
		for (int i = 0; i < array.size(); i++) {
			coordinates[i]  = parseCoordinate(array.get(i).getAsJsonArray());
		}
		return coordinates;
	}

	private Point parsePoint(JsonArray coord, GeometryFactory factory) {
		return factory.createPoint(parseCoordinate(coord));
	}

	private MultiPoint parseMultiPoint(JsonArray coords, GeometryFactory factory) {
		return factory.createMultiPointFromCoords(parseCoordinateArray(coords));
	}

	private LineString parseLineString(JsonArray coords, GeometryFactory factory) {
		return factory.createLineString(parseCoordinateArray(coords));
	}

	private MultiLineString parseMultiLineString(JsonArray coords, GeometryFactory factory) {
		LineString[] lineStrings = new LineString[coords.size()];
		for (int i = 0; i < coords.size(); i++) {
			JsonArray array = coords.get(i).getAsJsonArray();
			lineStrings[i] = factory.createLineString(parseCoordinateArray(array));
		}
		return factory.createMultiLineString(lineStrings);
	}

	private Polygon parsePolygon(JsonArray coords, GeometryFactory factory) {
		int n = coords.size();
		if (n == 0)
			return factory.createPolygon();
		JsonArray array = coords.get(0).getAsJsonArray();
		LinearRing shell = factory.createLinearRing(parseCoordinateArray(array));
		if (n == 1)
			return factory.createPolygon(shell);
		LinearRing[] holes = new LinearRing[n-1];
		for (int i = 1; i < n; i++) {
			array = coords.get(i).getAsJsonArray();
			holes[i-1] = factory.createLinearRing(parseCoordinateArray(array));
		}
		return factory.createPolygon(shell, holes);
	}

	private MultiPolygon parseMultiPolygon(JsonArray coords, GeometryFactory factory) {
		int n = coords.size();
		Polygon[]  polygons = new Polygon[n];
		for (int i = 0; i < n; i++) {
			polygons[i] = parsePolygon(coords.get(i).getAsJsonArray(), factory);
		}
		return factory.createMultiPolygon(polygons);
	}

	private GeometryCollection parseGeometryCollection(JsonObject obj, GeometryFactory factory) {
		JsonArray array = obj.get("geometries").getAsJsonArray();
		List<Geometry> geometries = new ArrayList<>();
		for (int i = 0; i < array.size(); i++)
			geometries.add(parseGeometry(array.get(i).getAsJsonObject(), factory));
		return new GeometryCollection(geometries.toArray(new Geometry[0]), factory);
	}
	
}