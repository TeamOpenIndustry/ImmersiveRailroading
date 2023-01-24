package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

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
	    return TextUtil.translate("track.immersiverailroading:class." + super.toString().toLowerCase(Locale.ROOT));
	}

	public boolean hasQuarters() {
		switch (this) {
			case TURN:
			case SWITCH:
				return true;
			default:
				return false;
		}
	}

	public boolean hasCurvosity() {
		switch (this) {
			case SWITCH:
			case CUSTOM:
				return true;
			default:
				return false;
		}
	}

	public boolean hasSmoothing() {
		switch (this) {
			case SLOPE:
			case TURN:
			case SWITCH:
			case CUSTOM:
				return true;
			default:
				return false;
		}
	}

	public boolean hasDirection() {
		switch (this) {
			case TURN:
			case SWITCH:
			case CUSTOM:
				return true;
			default:
				return false;
		}
	}
}