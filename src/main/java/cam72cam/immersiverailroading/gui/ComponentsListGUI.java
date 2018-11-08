package cam72cam.immersiverailroading.gui;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ComponentsListGUI extends GuiScreen{

	private static ResourceLocation bookTexture = new ResourceLocation("immersiverailroading:gui/components_list.png");
	private final int bookImageHeight = 192;
    private final int bookImageWidth = 192;
    private static String[] stringPageText = new String[4];
    private GuiButton buttonDone;
    
    
	public ComponentsListGUI() {
		
	}
	
	@Override
	public void initGui() {
		System.out.println("initGUI");
		int offsetFromScreenLeft = (width - bookImageWidth ) / 2;
		int offsetFromScreenTop = (height - bookImageHeight ) / 2;
		
		buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        
        buttonDone = new GuiButton(0, offsetFromScreenLeft, offsetFromScreenTop + bookImageHeight + 4, bookImageWidth, 20, "Done");
        buttonList.add(buttonDone);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		int offsetFromScreenLeft = (width - bookImageWidth ) / 2;
		int offsetFromScreenTop = (height - bookImageHeight ) / 2;
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(bookTexture);
		drawTexturedModalRect(offsetFromScreenLeft, offsetFromScreenTop, 0, 0, bookImageWidth,  bookImageHeight);

        Minecraft.getMinecraft().fontRenderer.drawString("test", offsetFromScreenLeft + 36, 32 + offsetFromScreenTop, 0xFFFFFF);
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
	
}
