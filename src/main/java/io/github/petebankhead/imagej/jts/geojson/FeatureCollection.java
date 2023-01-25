package io.github.petebankhead.imagej.jts.geojson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FeatureCollection {

	private String type = "FeatureCollection";
	private List<Feature> features;
	
	private FeatureCollection(Collection<? extends Feature> features) {
		this.features = new ArrayList<>(features);
	}
	
	public List<Feature> getFeatures() {
		return Collections.unmodifiableList(features);
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return "FeatureCollection [type=" + type + ", features=" + features + "]";
	}

	public static FeatureCollection wrap(Feature feature) {
		return wrap(Arrays.asList(feature));
	}
	
	public static FeatureCollection wrap(Collection<? extends Feature> features) {
		return new FeatureCollection(features);
	}
	
}
