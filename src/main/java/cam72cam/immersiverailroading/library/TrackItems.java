package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum TrackItems {
	STRAIGHT(0),
	CROSSING(1),
	SLOPE(2),
	TURN(4),// Legacy
	SWITCH(6),
	TURNTABLE(7),
	CUSTOM(9),
	TRANSFERTABLE(8),
	TURN_V2(3),// TODO: BuilderSwitch
	CUBICPARABOLA(5);

	private final int order;

	TrackItems(int order){
		this.order = order;
	}
	
	@Override
	public String toString() {
	    return TextUtil.translate("track.immersiverailroading:class." + super.toString().toLowerCase(Locale.ROOT));
	}

	public boolean hasQuarters() {
		switch (this) {
			case TURN:
			case TURN_V2:
			case SWITCH:
			case CUBICPARABOLA:
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
			case TURN_V2:
			case SWITCH:
			case CUSTOM:
			case CUBICPARABOLA:
				return true;
			default:
				return false;
		}
	}

	public boolean canRoll() {
		switch (this) {
			case SLOPE:
			case TURN:
			case TURN_V2:
			case STRAIGHT:
			case CUSTOM:
			case CUBICPARABOLA:
				return true;
			default:
				return false;
		}
	}

	public boolean hasDirection() {
		switch (this) {
			case TURN:
			case TURN_V2:
			case SWITCH:
			case CUBICPARABOLA:
				return true;
			default:
				return false;
		}
	}

	public boolean isTable() {
		switch (this){
			case TURNTABLE:
			case TRANSFERTABLE:
				return true;
			default:
				return false;
		}
	}

	public boolean isTransitionCurve() {
		switch (this) {
			case CUBICPARABOLA:
				return true;
			default:
				return false;
		}
	}

	public int getOrder() {
		return this.order;
	}
}