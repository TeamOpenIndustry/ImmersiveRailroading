package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;

import static cam72cam.immersiverailroading.gui.markdown.Colors.*;

/**
 * Render hover tooltips for manual
 * <p>
 * Maybe it can be considered as an API? Need more research
 */
public class ManualHoverRenderer {
    public static int mouseX;
    public static int mouseY;

    /**
     * Core method to render tooltip, currently only supports single-line tooltip
     * @param text Tooltip's text
     * @param bottomBound The expected bottom bound of the tip, if intersects then the tooltip will be rendered on the top of the mouse
     */
    public static void renderTooltip(String text, int bottomBound){
        if(text != null && !text.isEmpty()){
            int textWidth = GUIHelpers.getTextWidth(text);

            int x = mouseX;
            int y = mouseY;
            if(mouseY + 20 >= bottomBound){//Not enough space downward, render on the top
                y -= 18;
            }
            if(mouseX + textWidth > GUIHelpers.getScreenWidth()){//Not enough space on the right, render on the left
                x -= textWidth;
            }

            GUIHelpers.drawRect(x - 2, y + 1, textWidth + 4, 14, HOVER_BOUNDARY_COLOR);
            GUIHelpers.drawRect(x - 1, y + 2, textWidth + 2, 12, HOVER_INTERNAL_COLOR);
            GUIHelpers.drawString(text, x, y + 3, DEFAULT_TEXT_COLOR);
        }
    }

    /**
     * Update mouse's position on every move
     */
    public static void updateMousePosition(ClientEvents.MouseGuiEvent evt) {
        mouseX = evt.x;
        mouseY = evt.y;
    }
}
