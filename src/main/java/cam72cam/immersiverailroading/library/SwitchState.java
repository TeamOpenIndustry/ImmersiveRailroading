package cam72cam.immersiverailroading.library;

import cam72cam.umc.api.text.TextUtil;

import java.util.Locale;

public enum SwitchState {
	NONE,
	STRAIGHT,
	TURN;

	@Override
	public String toString() {
		return TextUtil.translate("immersiverailroading:switch_state." + super.toString().toLowerCase(Locale.ROOT));
	}
}
