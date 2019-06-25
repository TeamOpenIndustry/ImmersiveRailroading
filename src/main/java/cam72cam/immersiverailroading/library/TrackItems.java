package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum TrackItems {
	STRAIGHT,
	CROSSING,
	SLOPE,
	TURN,
	SWITCH,
	TURNTABLE,
	CUSTOM,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:class." + super.toString().toLowerCase()); 
	}
}