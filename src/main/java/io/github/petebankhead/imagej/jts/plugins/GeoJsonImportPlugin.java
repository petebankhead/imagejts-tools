package io.github.petebankhead.imagej.jts.plugins;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import io.github.petebankhead.imagej.jts.converters.GeometryToRoiConverter;
import io.github.petebankhead.imagej.jts.geojson.Feature;
import io.github.petebankhead.imagej.jts.geojson.FeatureCollection;
import io.github.petebankhead.imagej.jts.geojson.GsonUtils;

public class GeoJsonImportPlugin implements PlugIn {

	@Override
	public void run(String arg) {
		
		OpenDialog dialog = new OpenDialog("Import GeoJSON");
		String path = dialog.getPath();
		if (path == null)
			return;
		
		Gson gson = GsonUtils.newBuilder()
				.create();
		
		List<Roi> rois = null;
		try (Reader reader = Files.newBufferedReader(Paths.get(path))) {
			JsonElement element = gson.fromJson(reader, JsonElement.class);
			if (element.isJsonObject()) {
				JsonObject obj = element.getAsJsonObject();
				if (obj.has("type")) {
					String type = obj.get("type").getAsString().toLowerCase().trim();
					if ("featurecollection".equals(type)) {
						List<Feature> features = gson.fromJson(obj, FeatureCollection.class).getFeatures();
						rois = features.stream().map(f -> GeometryToRoiConverter.convertToRoi(f)).collect(Collectors.toList());
					} else if ("feature".equals(type)) {
						Feature feature = gson.fromJson(obj, Feature.class);
						rois = Collections.singletonList(GeometryToRoiConverter.convertToRoi(feature));
					} else {
						Geometry geometry = gson.fromJson(obj, Geometry.class);
						rois = Collections.singletonList(GeometryToRoiConverter.convertToRoi(geometry));
					}
				}
			}
			if (rois != null) {
				arg = arg.toLowerCase().trim();
				if ("roimanager".equals(arg)) {
					RoiManager rm = RoiManager.getInstance();
					if (rm == null)
						rm = new RoiManager();
					for (Roi r : rois)
						rm.addRoi(r);
					rm.setVisible(true);
				} else if ("overlay".equals(arg)) {
					ImagePlus imp = IJ.getImage();
					if (imp == null) {
						IJ.noImage();
						return;
					}
					Overlay overlay = imp.getOverlay();
					if (overlay == null)
						overlay = new Overlay();
					for (Roi r : rois) {
						overlay.add(r);
					}
					imp.setOverlay(overlay);
				} else if (rois.size() == 1 && IJ.getImage() != null) {
					IJ.getImage().setRoi(rois.get(0));
				} else {
					IJ.error("arg input should be 'overlay' or 'roimanager'!");					
				}
			}
				
		} catch (IOException e) {
			IJ.handleException(e);
		}
	}


}
