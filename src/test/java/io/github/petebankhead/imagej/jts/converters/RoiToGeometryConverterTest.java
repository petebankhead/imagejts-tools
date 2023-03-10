package io.github.petebankhead.imagej.jts.converters;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.Puntal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import io.github.petebankhead.imagej.jts.geojson.Feature;
import io.github.petebankhead.imagej.jts.geojson.FeatureCollection;
import io.github.petebankhead.imagej.jts.geojson.GeometryTypeAdapter;
import io.github.petebankhead.imagej.jts.geojson.GsonUtils;

class RoiToGeometryConverterTest {

	@ParameterizedTest
	@MethodSource("createRois")
	void testConvertRoi(Roi roi) {
		
		RoiToGeometryConverter converter = new RoiToGeometryConverter();
		
		Geometry geom = converter.roiToGeometry(roi);
		if (roi.isArea()) {
			assertTrue(geom instanceof Polygonal);
		} else if (roi.isLine()) {
			assertTrue(geom instanceof Lineal);
			assertEquals(roi.getLength(), geom.getLength(), 0.1);
		} else {
			assertTrue(geom instanceof Puntal);
			assertEquals(roi.getFloatPolygon().npoints, geom.getNumPoints());
		}
		
		Roi roi2 = new GeometryToRoiConverter().geometryToRoi(geom);
		System.out.println(roi + " -> " + roi2);
		
		// TODO: Consider EllipseRois separately - x/y base doesn't mean quite the same thing
		// and geometry conversion involves switching type to polygon
		if (!(roi instanceof EllipseRoi)) {
			assertEquals(roi.getXBase(), roi2.getXBase(), 0.1);
			assertEquals(roi.getYBase(), roi2.getYBase(), 0.1);
		}
		
	}
	
	
	@ParameterizedTest
	@MethodSource("createRois")
	void setSerializeRoiToJson(Roi roi) {
		
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.serializeSpecialFloatingPointValues()
				.registerTypeHierarchyAdapter(Geometry.class, new GeometryTypeAdapter())
				.create();
			
		roi.setGroup(2);
		roi.setStrokeWidth(4.0);
		roi.setStrokeColor(Color.RED);
		roi.setFillColor(Color.BLUE);
		roi.setPosition(2, 3, 4);
		roi.setName(UUID.randomUUID().toString());
		String json = gson.toJson(RoiToGeometryConverter.convertToFeature(roi));
//		System.err.println(json);
		assertNotNull(json);
		
		Feature feature = gson.fromJson(json, Feature.class);
		Roi roiNew = GeometryToRoiConverter.convertToRoi(feature);
		
		assertEquals(roi.getStrokeWidth(), roiNew.getStrokeWidth());
		assertEquals(roi.getName(), roiNew.getName());
		assertEquals(roi.getGroup(), roiNew.getGroup());
		assertEquals(roi.getStrokeColor(), roiNew.getStrokeColor());
		assertEquals(roi.getFillColor(), roiNew.getFillColor());
//		System.err.println(feature);
	}
	
	
	@Test
	void serializeFeatureCollection() {
		FeatureCollection featureCollection = RoiToGeometryConverter.convertToFeatureCollection(createRois());
		Gson gson = GsonUtils.newBuilder()
				.setPrettyPrinting()
				.create();
		String json = gson.toJson(featureCollection);
		System.err.println(json);
		
	}
	
	
	
	
	static List<Roi> createRois() {
		return Arrays.asList(
				new Roi(10, 20, 40, 50),
				new OvalRoi(10, 20, 40, 50),
				new EllipseRoi(10, 20, 40, 50, 0.6),
				new PointRoi(43, 84.0),
				new Line(40, 23, 93.4, 23.4),
				new PolygonRoi(
						new float[] {0.5f, 2.3f},
						new float[] {115.3f, 341.9f},
						Roi.POLYLINE
						),
				new PolygonRoi(
						new float[] {1, 10, 1, 10},
						new float[] {1, 1, 10, 10},
						Roi.POLYGON
						),
				new PointRoi(
						new float[] {0.5f, 2.3f},
						new float[] {115.3f, 341.9f}
						)
				);
	}
	
	
	

}
