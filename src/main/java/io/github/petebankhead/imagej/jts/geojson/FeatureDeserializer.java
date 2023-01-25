package io.github.petebankhead.imagej.jts.geojson;

import java.lang.reflect.Type;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class FeatureDeserializer implements JsonDeserializer<Feature> {

	@Override
	public Feature deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		
		if (json.isJsonNull())
			return null;
		
		JsonObject objFeature = json.getAsJsonObject();
		JsonObject objGeometry = objFeature.get("geometry").getAsJsonObject();
		
		// This is QuPath's fault... it stores the 'plane' outside the 'properties'
		JsonObject objProperties = objFeature.has("properties") ? 
				objFeature.get("properties").getAsJsonObject() :
					new JsonObject();
		if (objGeometry.has("plane"))
			objProperties.add("plane", objGeometry.get("plane"));
		
		Geometry geometry = context.deserialize(objGeometry, Geometry.class);
		Map<String, Object> properties = context.deserialize(objProperties, Map.class);

		Feature feature = Feature.create(geometry, properties);
		System.err.println(feature);
		return feature;
	}
	
}