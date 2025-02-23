package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.text.TextColor;

import java.util.Set;

class MarkdownElement {
    public final String rawText;

    public MarkdownElement(String rawText, Set<MarkdownStyle> styles) {
        for (MarkdownStyle style : styles) {
            for (TextColor color : style.wrapper) {
                rawText = color.wrap(rawText);
            }
        }
        this.rawText = rawText;
    }

    @Override
    public String toString() {
        return rawText;
    }
}
