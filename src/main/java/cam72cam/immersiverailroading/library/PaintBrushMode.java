package cam72cam.immersiverailroading.library;

import cam72cam.mod.text.TextUtil;

import java.util.Locale;

public enum PaintBrushMode {
	SEQUENTIAL,
	RANDOM,
	;

	@Override
	public String toString() {
		return ChatText.BRUSH_MODE_NAME_PREFIX.getRaw() + super.toString().toLowerCase(Locale.ROOT);
	}

	public String toTranslatedString() {
		return TextUtil.translate(this.toString());
	}
}
