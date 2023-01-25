package io.github.petebankhead.imagej.jts.plugins;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferParameters;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import io.github.petebankhead.imagej.jts.converters.GeometryToRoiConverter;
import io.github.petebankhead.imagej.jts.converters.RoiToGeometryConverter;

public class BufferRoisPlugin implements PlugIn {
	
	private static final String TITLE = "Buffer ROIs";
	
	private static enum EndCapStyle {
		ROUND(BufferParameters.CAP_ROUND),
		FLAT(BufferParameters.CAP_FLAT),
		SQUARE(BufferParameters.CAP_SQUARE);
		
		private int code;
		
		private EndCapStyle(int code) {
			this.code = code;
		}
		
		public int getCapStyleCode() {
			return code;
		}
		
		public String toString() {
			String s = super.toString();
			return s.charAt(0) + s.substring(1).toLowerCase();
		}
		
	}
	
	private EndCapStyle capStyle = EndCapStyle.ROUND;
	private double distance = 1.0;
	private boolean subtractInterior = false;
	private boolean keepOriginal = false;
	private boolean doOverlay = false;

	@Override
	public void run(String arg) {
		
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}
		boolean hasOverlay = imp.getOverlay() != null;
		boolean hasRoi = imp.getRoi() != null;
		if (!hasOverlay && !hasRoi) {
			IJ.error(TITLE, "No ROI or overlay found!");
			return;
		}
		
		GenericDialog gd = new GenericDialog(TITLE);
		gd.addSlider("Distance", -100, 100, distance, 0.5);
		gd.addEnumChoice("End_cap_style", capStyle);
		gd.addCheckbox("Subtract_interior", subtractInterior);
		gd.addCheckbox("Keep_original", keepOriginal);
		gd.addCheckbox("Do_overlay", doOverlay);
		gd.showDialog();
		
		if (gd.wasCanceled())
			return;
		
		distance = gd.getNextNumber();
		capStyle = gd.getNextEnumChoice(EndCapStyle.class);
		subtractInterior = gd.getNextBoolean();
		keepOriginal = gd.getNextBoolean();
		doOverlay = gd.getNextBoolean();
		
		RoiBufferer bufferer = new RoiBufferer()
				.distance(distance)
				.subtractInterior(subtractInterior)
				.capStyle(capStyle);
		
		if (hasOverlay && doOverlay) {
			Overlay overlay = imp.getOverlay();
			Roi[] rois = overlay.toArray();
			if (!keepOriginal)
				overlay.clear();
			for (Roi roi : rois) {
				Roi roiBuffered = bufferer.buffer(roi);
				if (roiBuffered != null)
					overlay.add(roiBuffered);
			}
			imp.setOverlay(overlay);
		} else if (hasRoi) {
			Roi roi = imp.getRoi();
			Roi roi2 = bufferer.buffer(imp.getRoi());
			if (roi2 == null) {
				IJ.showStatus("No ROI remains after buffering!");
				if (!keepOriginal)
					imp.killRoi();
			} else {
				if (keepOriginal) {
					Overlay overlay = imp.getOverlay();
					if (overlay == null)
						overlay = new Overlay();
					overlay.add(roi);
					imp.setOverlay(overlay);
				} else
					imp.setRoi(roi2);
			}
		}
	}
	

	public static Overlay bufferOverlay(Overlay overlay, double distance) {
		return bufferOverlay(overlay, distance, BufferParameters.CAP_ROUND);
	}
	
	public static Overlay bufferOverlay(Overlay overlay, double distance, int endCapStyle) {
		Overlay overlayUpdated = new Overlay();
		for (int i = 0; i < overlay.size(); i++) {
			Roi roi = overlay.get(i);
			Roi roiUpdated = bufferRoi(roi, distance, endCapStyle);
			if (roiUpdated != null)
				overlayUpdated.add(roiUpdated);
		}
		return overlayUpdated;
	}
	
	public static Roi bufferRoi(Roi roi, double distance) {
		return bufferRoi(roi, distance, BufferParameters.CAP_ROUND);
	}
	
	
	public static Roi bufferRoi(Roi roi, double distance, int endCapStyle) {
		Geometry geometry = RoiToGeometryConverter.convertToGeometry(roi);
		Geometry geometryBuffered = geometry.buffer(distance, BufferParameters.DEFAULT_QUADRANT_SEGMENTS, endCapStyle);
		if (geometryBuffered.isEmpty())
			return null;
		Roi roiBuffered = GeometryToRoiConverter.convertToRoi(geometryBuffered);
		roiBuffered.copyAttributes(roi);
		return roiBuffered;
	}
	
	
	private static class RoiBufferer {
		
		private EndCapStyle capStyle = EndCapStyle.ROUND;
		private double distance = 1.0;
		private boolean subtractInterior = false;
		
		RoiBufferer capStyle(EndCapStyle style) {
			this.capStyle = style;
			return this;
		}
		
		RoiBufferer distance(double distance) {
			this.distance = distance;
			return this;
		}
		
		RoiBufferer subtractInterior(boolean doSubtract) {
			this.subtractInterior = doSubtract;
			return this;
		}
		
		
		Roi buffer(Roi roi) {
			Geometry geometry = RoiToGeometryConverter.convertToGeometry(roi);
			Geometry geometryBuffered = geometry.buffer(distance, BufferParameters.DEFAULT_QUADRANT_SEGMENTS, capStyle.getCapStyleCode());
			if (subtractInterior)
				geometryBuffered = geometry.symDifference(geometryBuffered);
			if (geometryBuffered.isEmpty())
				return null;
			Roi roiBuffered = GeometryToRoiConverter.convertToRoi(geometryBuffered);
			roiBuffered.copyAttributes(roi);
			return roiBuffered;
		}
		
	}
	
	

}
