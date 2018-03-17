package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;

public enum LocoControlMode {
	THROTTLE_FORWARD,
	THROTTLE_REVERSE,
	BRAKE,
	HORN,
	COMPUTER,
	;
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:loco_control_mode." + super.toString().toLowerCase()); 
	}
}
