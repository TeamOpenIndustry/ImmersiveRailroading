package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui.helpers.GUIHelpers;

public class ManualHoverRenderer {
    public static int mouseX;
    public static int mouseY;

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

            GUIHelpers.drawRect(x - 2, y + 1, textWidth + 4, 14, 0xFF555555);
            GUIHelpers.drawRect(x - 1, y + 2, textWidth + 2, 12, 0xFFFFFFFF);
            GUIHelpers.drawString(text, x, y + 3, 0xFF000000);
        }
    }

    public static void updateMousePosition(ClientEvents.MouseGuiEvent evt) {
        mouseX = evt.x;
        mouseY = evt.y;
    }
}
