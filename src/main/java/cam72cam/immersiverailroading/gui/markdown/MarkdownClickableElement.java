package cam72cam.immersiverailroading.gui.markdown;

import java.awt.geom.Rectangle2D;

public abstract class MarkdownClickableElement extends MarkdownElement{
    public Rectangle2D section;

    /**
     * Called upon click inside section
     */
    public abstract void click();

    /**
     * Called when mouse is over
     * @param bottomBound Param for internal use
     */
    public abstract void renderTooltip(int bottomBound);
}
