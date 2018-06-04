package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum TrackDirection {
	NONE,
	LEFT,
	RIGHT;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:direction." + super.toString().toLowerCase()); 
	}
}
