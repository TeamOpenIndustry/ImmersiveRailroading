package cam72cam.immersiverailroading.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiSlider;

public class GuiScrollBar extends GuiSlider {

	
	public GuiScrollBar(int id, int xPos, int yPos, int width, int height, String displayStr, double minVal, double maxVal, double currentVal, ISlider par) {
		super(id, xPos, yPos, width, height, displayStr, displayStr, minVal, maxVal, currentVal, true, false, par);
	}

	@Override
	protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            if (this.dragging)
            {
                this.sliderValue = (mouseY - (this.y + 4)) / (float)(this.height - 8);
                updateSlider();
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.x, this.y + (int)(this.sliderValue * (float)(this.height - 8)), 0, 66, 20, 20);
        }
    }
	
	@Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        if (super.mousePressed(mc, mouseX, mouseY))
        {
            this.sliderValue = (float)(mouseY - (this.y + 4)) / (float)(this.height - 8);
            updateSlider();
            this.dragging = true;
            return true;
        }
        else
        {
            return false;
        }
    }
}
