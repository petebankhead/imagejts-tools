package io.github.petebankhead.imagej.jts.geojson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class FeatureCollection {

	private String type = "FeatureCollection";
	private List<Feature> features;
	
	private FeatureCollection(Collection<? extends Feature> features) {
		this.features = new ArrayList<>(features);
	}
	
	
	public static FeatureCollection wrap(Feature feature) {
		return wrap(Arrays.asList(feature));
	}
	
	public static FeatureCollection wrap(Collection<? extends Feature> features) {
		return new FeatureCollection(features);
	}
	
}
