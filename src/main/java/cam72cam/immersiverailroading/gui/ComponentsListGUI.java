package cam72cam.immersiverailroading.gui;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

public class ComponentsListGUI extends GuiScreen{

	private static ResourceLocation bookTexture = new ResourceLocation("immersiverailroading:gui/components_list.png");
	
	public ComponentsListGUI() {
		
	}
	
	@Override
	public void initGui() {
		System.out.println("initGUI");
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		System.out.println("drawScreen");
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(bookTexture);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
}
