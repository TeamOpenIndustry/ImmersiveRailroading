package cam72cam.mod.text;

import net.minecraft.util.text.translation.I18n;

@SuppressWarnings("deprecation")
public class TextUtil {
	public static String translate(String name) {
		return I18n.translateToLocal(name);
	}

	public static String translate(String name, Object[] objects) {
		return I18n.translateToLocalFormatted(name, objects);
	}
}
