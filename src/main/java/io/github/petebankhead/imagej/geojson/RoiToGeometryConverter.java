package io.github.petebankhead.imagej.geojson;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.locationtech.jts.awt.ShapeReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.util.GeometricShapeFactory;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;


public class RoiToGeometryConverter {
	
	private GeometryFactory factory;
	
	private double flatness = 0.1;
	
	public RoiToGeometryConverter() {
		this(new GeometryFactory(
			new PrecisionModel(100.0)
			));
	}
	
	public RoiToGeometryConverter(GeometryFactory factory) {
		Objects.requireNonNull(factory, "GeometryFactory must not be null!");
		this.factory = factory;
	}
	
	public void setFlatness(double flatness) {
		this.flatness = flatness;
	}
	
	public double getFlatness() {
		return flatness;
	}
	
	public Geometry roiToGeometry(Roi roi) {
		switch (roi.getType()) {
		case Roi.RECTANGLE:
			return createRectangle(roi);
		case Roi.POINT:
			return createPoints(roi.getFloatPolygon());
		case Roi.LINE:
		case Roi.POLYLINE:
		case Roi.FREELINE:
			return createLineString(roi.getFloatPolygon());
		default:
			Shape shape = new ShapeRoi(roi).getShape();
			return shapeToGeometry(shape, null);
		}
	}
	
	private Geometry shapeToGeometry(Shape shape, AffineTransform transform) {
		if (shape instanceof Area)
    		return areaToGeometry((Area)shape, transform);
    	PathIterator iterator = shape.getPathIterator(transform, flatness);
    	if ((shape instanceof Path2D || shape instanceof GeneralPath) && containsClosedSegments(iterator)) {
    		// Arbitrary paths that correspond to an area can fail with JTS ShapeReader, so convert to areas instead
    		return shapeToGeometry(new Area(shape), transform);
    	} else
    		iterator = shape.getPathIterator(transform, flatness);
    	return new ShapeReader(factory).read(iterator);
	}
	

	private Geometry areaToGeometry(Area area, AffineTransform transform) {
		PathIterator iter = area.getPathIterator(transform, flatness);

    	PrecisionModel precisionModel = factory.getPrecisionModel();
    	Polygonizer polygonizer = new Polygonizer(true);

    	List<Coordinate[]> coords = (List<Coordinate[]>)ShapeReader.toCoordinates(iter);
    	List<Geometry> geometries = new ArrayList<>();
    	for (Coordinate[] array : coords) {
    		for (Coordinate c : array)
    			precisionModel.makePrecise(c);

    		LineString lineString = factory.createLineString(array);
    		geometries.add(lineString);
    	}
    	polygonizer.add(factory.buildGeometry(geometries).union());
    	return polygonizer.getGeometry();
	}
	
	/**
     * Test of an iterator contains closed segments, indicating the iterator relates to an area.
     * @param iterator
     * @return true if any SEG_CLOSE segments are found
     */
    private boolean containsClosedSegments(PathIterator iterator) {
    	double[] coords = new double[6];
    	while (!iterator.isDone()) {
    		iterator.next();
    		if (iterator.currentSegment(coords) == PathIterator.SEG_CLOSE)
    			return true;
    	}
    	return false;
    }
	

	private Geometry createLineString(FloatPolygon polygon) {
		Coordinate[] coords = toCoordinateArray(polygon);
		return factory.createLineString(coords);
	}
	
	private Geometry createPoints(FloatPolygon polygon) {
		Coordinate[] coords = toCoordinateArray(polygon);
		if (coords.length == 1)
			return factory.createPoint(coords[0]);
		else
			return factory.createMultiPointFromCoords(coords);
	}
	
	
	private Coordinate[] toCoordinateArray(FloatPolygon polygon) {
		Coordinate[] coords = new Coordinate[polygon.npoints];
		PrecisionModel pm = factory.getPrecisionModel();
		for (int i = 0; i < coords.length; i++) {
			coords[i] = new Coordinate(
					pm.makePrecise(polygon.xpoints[i]),
					pm.makePrecise(polygon.ypoints[i])
					);
		}
		return coords;
	}
	
	
	private Geometry createRectangle(Roi roi) {
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(factory);
		shapeFactory.setEnvelope(roiBoundsToEnvelope(roi));
		shapeFactory.setNumPoints(4);
		return shapeFactory.createRectangle();
	}
	
	private Envelope roiBoundsToEnvelope(Roi roi) {
		Rectangle2D.Double bounds = roi.getFloatBounds();
		return new Envelope(
				bounds.getMinX(), bounds.getMaxX(),
				bounds.getMinY(), bounds.getMaxY()
				);
	}
	
	
}
