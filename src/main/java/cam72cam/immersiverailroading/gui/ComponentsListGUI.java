package cam72cam.immersiverailroading.gui;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

public class ComponentsListGUI extends GuiScreen{

	private static ResourceLocation bookTexture;
	
	public ComponentsListGUI() {
		bookTexture = new ResourceLocation(ImmersiveRailroading.MODID + ":gui/components_list.png");
	}
	
	@Override
	public void initGui() {

	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(bookTexture);
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
}
