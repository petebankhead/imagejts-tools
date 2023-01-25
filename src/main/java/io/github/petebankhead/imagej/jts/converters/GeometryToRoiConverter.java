package io.github.petebankhead.imagej.jts.converters;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
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


public class GeometryToRoiConverter {
	
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
