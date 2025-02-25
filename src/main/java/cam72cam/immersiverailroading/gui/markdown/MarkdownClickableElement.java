package cam72cam.immersiverailroading.gui.markdown;

import java.awt.geom.Rectangle2D;

public abstract class MarkdownClickableElement extends MarkdownElement{
    public Rectangle2D section;

    public abstract void click();

    public abstract void renderTooltip(int bottomBound);
}
