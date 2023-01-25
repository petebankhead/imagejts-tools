package io.github.petebankhead.imagej.jts.geojson;

import java.util.Collections;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;

public class Feature {
	
	private final String type = "feature";
	private final Geometry geometry;
	
	private final Map<String, ?> properties;
	
	private Feature(Geometry geometry, Map<String, ?> properties) {
		this.geometry = geometry;
		this.properties = properties;
	}
	
	public String getType() {
		return type;
	}
	
	public Geometry getGeometry() {
		return geometry;
	}
	
	public Map<String, ?> getProperties() {
		if (properties == null)
			return Collections.emptyMap();
		else
			return Collections.unmodifiableMap(properties);
	}
	
	
	@Override
	public String toString() {
		return "Feature [type=" + type + ", geometry=" + geometry + ", properties=" + properties + "]";
	}

	public static Feature create(Geometry geometry, Map<String, ?> properties) {
		return new Feature(geometry, properties);
	}
	
	public static Feature create(Geometry geometry) {
		return create(geometry, null);
	}
	
}
