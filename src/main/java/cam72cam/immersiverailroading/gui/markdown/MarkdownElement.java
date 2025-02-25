package cam72cam.immersiverailroading.gui.markdown;

public abstract class MarkdownElement {
    public String text;

    /**
     * Apply this element to Renderable string
     */
    public abstract String apply();

    /**
     * Split this element into two smaller ones
     */
    public abstract MarkdownElement[] split(int splitPos);
}
