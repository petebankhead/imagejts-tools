package io.github.petebankhead.imagej.jts.converters;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.Puntal;

import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;
import io.github.petebankhead.imagej.jts.geojson.Feature;


public class GeometryToRoiConverter {
	
	
	public static Roi convertToRoi(Geometry geometry) {
		return new GeometryToRoiConverter().geometryToRoi(geometry);
	}
	
	
	public static Roi convertToRoi(Feature feature) {
		return new GeometryToRoiConverter().featureToRoi(feature);
	}
	
	
	public Roi featureToRoi(Feature feature) {
		Roi roi = geometryToRoi(feature.getGeometry());
		Map<String, ?> properties = feature.getProperties();
		
		Object imagejProperties = properties.getOrDefault("imagej", null);
		if (imagejProperties instanceof Map)
			applyImageJPropertiesToRoi(roi, (Map)imagejProperties);	
		
		Object planeProperties = properties.getOrDefault("plane", null);
		if (planeProperties instanceof Map)
			applyPlanePropertiesToRoi(roi, (Map)planeProperties);	

		return roi;
	}
	
	private static void applyImageJPropertiesToRoi(Roi roi, Map<?, ?> imageJProperties) {
		String name = tryToGetValue(imageJProperties, "name", String.class, roi.getName());
		Number group = tryToGetValue(imageJProperties, "group", Number.class, Roi.getDefaultGroup());
//		String roiType = tryToGetValue(imageJProperties, "type", String.class, null);
		Number position = tryToGetValue(imageJProperties, "position", Number.class, roi.getPosition());
		Number strokeWidth = tryToGetValue(imageJProperties, "strokeWidth", Number.class, roi.getStrokeWidth());
		Color strokeColor = tryToGetColor(imageJProperties, "strokeColor");
		Color fillColor = tryToGetColor(imageJProperties, "fillColor");
		
		roi.setName(name);
		roi.setGroup(group.intValue());
		roi.setPosition(position.intValue());
		roi.setStrokeWidth(strokeWidth.doubleValue());
		if (strokeColor != null)
			roi.setStrokeColor(strokeColor);
		if (fillColor != null)
			roi.setFillColor(fillColor);
	}
	
	private static void applyPlanePropertiesToRoi(Roi roi, Map<?, ?> planeProperties) {
		Number c = tryToGetValue(planeProperties, "c", Number.class, -1);
		Number z = tryToGetValue(planeProperties, "z", Number.class, -1);
		Number t = tryToGetValue(planeProperties, "t", Number.class, -1);
		roi.setPosition(c.intValue()+1, z.intValue()+1, t.intValue()+1);
	}
	
	private static <T> T tryToGetValue(Map<?, ?> map, String key, Class<T> cls, T defaultValue) {
		Object value = map.getOrDefault(key, null);
		if (value != null && cls.isAssignableFrom(value.getClass()))
			return (T)value;
		return defaultValue;
	}
	
	private static Color tryToGetColor(Map<?, ?> map, String key) {
		Object value = map.getOrDefault(key, null);
		int[] array = null;
		if (value instanceof List) {
			array = ((List<?>)value).stream()
				.filter(p -> p instanceof Number)
				.map(p -> (Number)p)
				.mapToInt(p -> p.intValue())
				.toArray();
		} else if (value instanceof int[])
			array = (int[])value;
		if (array != null) {
			if (array.length == 3)
				return new Color(array[0], array[1], array[2]);
			else if (array.length == 4)
				return new Color(array[0], array[1], array[2], array[3]);
		}
		return null;
	}
	
	
	public Roi geometryToRoi(Geometry geometry) {
		
		geometry = homogenizeGeometryCollection(geometry);
		
		if (geometry instanceof Point) {
			Point point = (Point)geometry;
			return new PointRoi(point.getX(), point.getY());
		} else if (geometry instanceof MultiPoint) {
			MultiPoint multipoint = (MultiPoint)geometry;
			Coordinate[] coordinates = multipoint.getCoordinates();
			return new PointRoi(coordinatesToFloatPolygon(coordinates));
		} else if (geometry instanceof LineString) {
			LineString multipoint = (LineString)geometry;
			Coordinate[] coordinates = multipoint.getCoordinates();
			if (coordinates.length == 2)
				return new Line(
						coordinates[0].x,
						coordinates[0].y,
						coordinates[1].x,
						coordinates[1].y
						);
			return new PolygonRoi(coordinatesToFloatPolygon(coordinates), Roi.POLYLINE);
		} else if (geometry instanceof MultiLineString) {
			throw new UnsupportedOperationException("ImageJ does not support MultiLineStrings!");
		} else if (geometry instanceof Polygonal) {
			Shape shape = new ShapeWriter().toShape(geometry);
			Area area = shape instanceof Area ? (Area)shape : new Area(shape);
			if (area.isRectangular()) {
				Rectangle2D bounds = area.getBounds2D();
				return new Roi(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
			}
			ShapeRoi shapeRoi = new ShapeRoi(area);
			Roi[] rois = shapeRoi.getRois();
			return rois.length == 1 ? rois[0] : shapeRoi;
		} else
			throw new UnsupportedOperationException("Unsupported geometry " + geometry);
	}
	
	
	private static FloatPolygon coordinatesToFloatPolygon(Coordinate[] coords) {
		float[] x = new float[coords.length];
		float[] y = new float[coords.length];
		for (int i = 0; i < coords.length; i++) {
			x[i] = (float)coords[i].getX();
			y[i] = (float)coords[i].getY();
		}
		return new FloatPolygon(x, y);
	}
	
	
	private static Geometry homogenizeGeometryCollection(Geometry geometry) {
    	if (geometry instanceof Polygonal || geometry instanceof Puntal || geometry instanceof Lineal) {
    		return geometry;
    	}
    	boolean hasPolygons = false;
    	boolean hasLines = false;
    	List<Geometry> collection = new ArrayList<>();
    	for (Geometry geom : flatten(geometry, null)) {
    		geom = homogenizeGeometryCollection(geom);
    		if (geom instanceof Polygonal) {
    			if (!hasPolygons)
    				collection.clear();
   				collection.add(geom);
    			hasPolygons = true;
    		} else if (geom instanceof Lineal) {
    			if (hasPolygons)
    				continue;
    			if (!hasLines)
    				collection.clear();
   				collection.add(geom);
    			hasLines = true;
    		} else if (geom instanceof Puntal) {
    			if (hasPolygons || hasLines)
    				continue;
    			collection.add(geom);
    		}
    	}
    	// Factory helps to ensure we have the correct type (e.g. MultiPolygon rather than GeometryCollection)
    	return geometry.getFactory().buildGeometry(collection);
    }
	
	private static List<Geometry> flatten(Geometry geometry, List<Geometry> list) {
    	if (list == null) {
    		list = new ArrayList<>();
    	}
    	for (int i = 0; i < geometry.getNumGeometries(); i++) {
    		Geometry geom = geometry.getGeometryN(i);
    		if (geom instanceof GeometryCollection)
    			flatten(geom, list);
    		else
    			list.add(geom);
    	}
    	return list;
    }
	
	
}
