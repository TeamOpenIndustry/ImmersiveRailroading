package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum TrackPositionType {
	FIXED,
	PIXELS,
	PIXELS_LOCKED,
	SMOOTH,
	SMOOTH_LOCKED,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:position." + super.toString().toLowerCase()); 
	}
}
