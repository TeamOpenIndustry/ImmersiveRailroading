package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum TrackPositionType {
	FIXED,
	PIXELS,
	SMOOTH;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:position." + super.toString().toLowerCase()); 
	}
}
