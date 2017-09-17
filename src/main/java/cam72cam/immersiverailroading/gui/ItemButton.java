package cam72cam.immersiverailroading.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

public class ItemButton extends GuiButton {

	public ItemStack stack;

	public ItemButton(int buttonId, ItemStack stack, int x, int y) {
		super(buttonId, x, y, 16, 16, "");
		this.stack = stack;
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		Gui.drawRect(x, y, x+16, y+16, 0xFFFFFFFF);
		RenderHelper.enableStandardItemLighting();

        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) {
        	font = mc.fontRenderer;
        }
		//mc.getRenderItem().renderItemIntoGUI(stack, x, y);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
		mc.getRenderItem().renderItemOverlays(font, stack, x, y);
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= this.x && mouseX < this.x + 16 && mouseY >= this.y && mouseY < this.y + 16;
	}
}
