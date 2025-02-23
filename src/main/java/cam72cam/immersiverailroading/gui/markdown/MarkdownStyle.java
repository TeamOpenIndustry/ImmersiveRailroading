package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.text.TextColor;

enum MarkdownStyle {
    BOLD(TextColor.BOLD),
    ITALIC(TextColor.ITALIC),
    STRIKETHROUGH(TextColor.STRIKETHROUGH),
    UNDERLINE(TextColor.UNDERLINE),
    URL(TextColor.BLUE, TextColor.UNDERLINE),
    SKIP();

    public TextColor[] wrapper;

    MarkdownStyle(TextColor... wrapper) {
        this.wrapper = wrapper;
    }
}
