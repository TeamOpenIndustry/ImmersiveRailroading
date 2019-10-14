package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

public enum SwitchState {
	NONE,
	STRAIGHT,
	TURN;

	@Override
	public String toString() {
		return TextUtil.translate("immersiverailroading:switch_state." + super.toString().toLowerCase());
	}
}
