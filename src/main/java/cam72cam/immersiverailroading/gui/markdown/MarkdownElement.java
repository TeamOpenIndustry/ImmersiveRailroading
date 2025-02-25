package cam72cam.immersiverailroading.gui.markdown;

import util.Matrix4;

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

    /**
     * Render the element and return its height
     */
    public abstract int render(Matrix4 transform, int pageWidth);
}
