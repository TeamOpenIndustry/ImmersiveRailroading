package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum LocoControlMode {
	THROTTLE_FORWARD,
	THROTTLE_REVERSE,
	BRAKE,
	HORN,
	COMPUTER,
	REVERSER_FORWARD,
	REVERSER_REVERSE,
	;
	
	@Override
	public String toString() {
	    return "immersiverailroading:loco_control_mode." + super.toString().toLowerCase(Locale.ROOT);
	}
}
