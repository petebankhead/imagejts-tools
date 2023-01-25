package io.github.petebankhead.imagej.jts.geojson;

import org.locationtech.jts.geom.Geometry;

import com.google.gson.GsonBuilder;

public class GsonUtils {
	
	public static GsonBuilder newBuilder() {
		return new GsonBuilder()
				.serializeSpecialFloatingPointValues()
				.registerTypeAdapter(Feature.class, new FeatureDeserializer())
				.registerTypeHierarchyAdapter(Geometry.class, new GeometryTypeAdapter());
	}
	

}
