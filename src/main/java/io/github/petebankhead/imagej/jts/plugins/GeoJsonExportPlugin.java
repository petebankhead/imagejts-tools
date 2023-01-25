package io.github.petebankhead.imagej.jts.plugins;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import io.github.petebankhead.imagej.jts.geojson.RoiTypeAdapter;

public class GeoJsonExportPlugin implements PlugIn {

	@Override
	public void run(String arg) {
		Objects.requireNonNull(arg, "arg must not be null!");
		arg = arg.toLowerCase().trim();
		
		if ("roi".equals(arg)) {
			promptToExportRoi();
		} else if ("overlay".equals(arg)) {
			promptToExportOverlay();			
		} else if ("roimanager".equals(arg)) {
			promptToExportRoiManager();						
		} else
			throw new IllegalArgumentException("GeoJSON export arg should be 'roi', 'overlay' or 'roimanager'");
	}
	
	
	private boolean promptToExportRoi() {
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.noImage();
			return false;
		}
		Roi roi = imp.getRoi();
		if (roi == null) {
			IJ.showMessage("No Roi to export!");
			return false;
		}
			
		Optional<String> path = promptForSavePath("Export Roi to GeoJSON");
		if (path.isPresent()) {
			try {
				exportRoiToGeoJson(roi, path.get());
				return true;
			} catch (IOException e) {
				IJ.handleException(e);
				return false;
			}
		} else
			return false;
	}
	
	private boolean promptToExportOverlay() {
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.noImage();
			return false;
		}
		Overlay overlay = imp.getOverlay();
		if (overlay == null) {
			IJ.showMessage("No Overlay to export!");
			return false;
		}
			
		Optional<String> path = promptForSavePath("Export Overlay to GeoJSON");
		if (path.isPresent()) {
			try {
				exportOverlayToGeoJson(overlay, path.get());
				return true;
			} catch (IOException e) {
				IJ.handleException(e);
				return false;
			}
		} else
			return false;
	}
	
	
	private boolean promptToExportRoiManager() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			IJ.showMessage("No RoiManager to export!");
			return false;
		}
			
		Optional<String> path = promptForSavePath("Export RoiManager to GeoJSON");
		if (path.isPresent()) {
			try {
				exportRoiManagerToGeoJson(rm, path.get());
				return true;
			} catch (IOException e) {
				IJ.handleException(e);
				return false;
			}
		} else
			return false;
	}
	
	
	private static Optional<String> promptForSavePath(String title) {
		SaveDialog dialog = new SaveDialog(title, null, ".geojson");
		String name = dialog.getFileName();
		if (name == null)
			return Optional.empty();
		else {
			File file = new File(dialog.getDirectory(), dialog.getFileName());
			return Optional.of(file.getAbsolutePath());
		}
	}
	
	
	private static Gson createGson() {
		return new GsonBuilder()
				.serializeSpecialFloatingPointValues()
				.setPrettyPrinting()
				.registerTypeHierarchyAdapter(Roi.class, new RoiTypeAdapter())
				.create();
	}
	
	public void exportRoiToGeoJson(Roi roi, String path) throws IOException {
		Gson gson = createGson();
		try (Writer writer = Files.newBufferedWriter(Paths.get(path))) {
			gson.toJson(roi, writer);			
		}
	}
	
	public void exportRoisToGeoJson(Collection<? extends Roi> rois, String path) throws IOException {
		Gson gson = createGson();
		Type exportType = TypeToken.getParameterized(List.class, Roi.class).getType();
		try (Writer writer = Files.newBufferedWriter(Paths.get(path))) {
			gson.toJson(rois, exportType, writer);
		}
	}
	
	public void exportOverlayToGeoJson(Overlay overlay, String path) throws IOException {
		List<Roi> rois = Arrays.asList(overlay.toArray());
		exportRoisToGeoJson(rois, path);
	}

	public void exportRoiManagerToGeoJson(RoiManager roiManager, String path) throws IOException {
		List<Roi> rois = Arrays.asList(roiManager.getRoisAsArray());
		exportRoisToGeoJson(rois, path);
	}


}
