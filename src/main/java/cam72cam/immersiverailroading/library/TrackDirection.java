package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum TrackDirection {
	NONE,
	RIGHT,
	LEFT;
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:direction." + super.toString().toLowerCase()); 
	}

	public float toYaw() {
		switch (this) {
			case LEFT:
				return 180;
			case RIGHT:
				return 0;
			case NONE:
			default:
				return 0;
		}
	}
}
