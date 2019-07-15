package cam72cam.mod.text;

import net.minecraft.util.text.TextFormatting;

public enum TextColor {
    BLACK(TextFormatting.BLACK),
    DARK_BLUE(TextFormatting.DARK_BLUE),
    DARK_GREEN(TextFormatting.DARK_GREEN),
    DARK_AQUA(TextFormatting.DARK_AQUA),
    DARK_RED(TextFormatting.DARK_RED),
    DARK_PURPLE(TextFormatting.DARK_PURPLE),
    GOLD(TextFormatting.GOLD),
    GRAY(TextFormatting.GRAY),
    DARK_GRAY(TextFormatting.DARK_GRAY),
    BLUE(TextFormatting.BLUE),
    GREEN(TextFormatting.GREEN),
    AQUA(TextFormatting.AQUA),
    RED(TextFormatting.RED),
    LIGHT_PURPLE(TextFormatting.LIGHT_PURPLE),
    YELLOW(TextFormatting.YELLOW),
    WHITE(TextFormatting.WHITE),
    OBFUSCATED(TextFormatting.OBFUSCATED),
    BOLD(TextFormatting.BOLD),
    STRIKETHROUGH(TextFormatting.STRIKETHROUGH),
    UNDERLINE(TextFormatting.UNDERLINE),
    ITALIC(TextFormatting.ITALIC),
    RESET(TextFormatting.RESET);
    public final TextFormatting internal;

    TextColor(TextFormatting color) {
        internal = color;
    }

    public String wrap(String text) {
        return internal + text + TextColor.RESET;
    }

    public String toString() {
        return internal.toString();
    }
}
