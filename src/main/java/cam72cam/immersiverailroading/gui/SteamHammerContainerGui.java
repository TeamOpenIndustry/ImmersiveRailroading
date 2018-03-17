package cam72cam.immersiverailroading.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidRegistry;

public class SteamHammerContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private TileMultiblock tile;

    public SteamHammerContainerGui(SteamHammerContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.tile = container.tile;
        this.horizSlots = 10;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }
    
    @Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null)
				this.mc.setIngameFocus();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int currY = j;
    	
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows);
    	
    	GL11.glPushMatrix();
    	int scale = 4;
    	GL11.glScaled(scale, scale, scale);
    	GL11.glPopMatrix();
    	
    	this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots, inventoryRows, FluidRegistry.LAVA, this.tile.getCraftProgress()/100f);
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	drawSlot(i + paddingLeft + slotSize * horizSlots - slotSize-5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}