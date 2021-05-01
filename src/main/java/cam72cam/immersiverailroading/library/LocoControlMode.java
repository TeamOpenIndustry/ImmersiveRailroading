package cam72cam.immersiverailroading.library;

public enum LocoControlMode {
	THROTTLE_FORWARD,
	THROTTLE_REVERSE,
	BRAKE,
	HORN,
	COMPUTER,
	;
	
	@Override
	public String toString() {
	    return "immersiverailroading:loco_control_mode." + super.toString().toLowerCase();
	}
}
