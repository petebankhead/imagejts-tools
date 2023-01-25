package io.github.petebankhead.imagej.jts.geojson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.Locale;

import org.locationtech.jts.geom.GeometryFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class FeatureTypeAdapter extends TypeAdapter<Feature> {
	
	private NumberFormat nf;
	private GeometryFactory factory = new GeometryFactory();
	
	public FeatureTypeAdapter() {
		this(3);
	}
	
	public FeatureTypeAdapter(int numDecimalPlaces) {
		nf = NumberFormat.getInstance(Locale.US);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(numDecimalPlaces);
	}
	
	
	@Override
	public void write(JsonWriter out, Feature value) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public Feature read(JsonReader in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	static class FeatureSerializer implements JsonSerializer<Feature> {

		@Override
		public JsonElement serialize(Feature src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub
			
			return null;
		}
		
	}
	
	static class FeatureDeserializer implements JsonDeserializer<Feature> {

		@Override
		public Feature deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	
}