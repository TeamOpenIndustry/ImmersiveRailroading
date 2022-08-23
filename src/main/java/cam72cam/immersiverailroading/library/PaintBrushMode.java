package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum PaintBrushMode {
	SEQUENTIAL,
	RANDOM,
	;

	private String BRUSH_MODE_NAME_PREFIX = "chat.immersiverailroading:brush.mode.";

	@Override
	public String toString() {
		return BRUSH_MODE_NAME_PREFIX + super.toString().toLowerCase(Locale.ROOT);
	}

	public String toTranslatedString() {
		return TextUtil.translate(this.toString());
	}
}
