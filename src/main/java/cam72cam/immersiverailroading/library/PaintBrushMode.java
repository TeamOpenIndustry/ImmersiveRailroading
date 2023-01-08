package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum PaintBrushMode {
	GUI,
	RANDOM_SINGLE,
	RANDOM_COUPLED,
	;

	@Override
	public String toString() {
		return "chat.immersiverailroading:brush.mode." + super.toString().toLowerCase(Locale.ROOT);
	}

	public String toTranslatedString() {
		return TextUtil.translate(this.toString());
	}
}
