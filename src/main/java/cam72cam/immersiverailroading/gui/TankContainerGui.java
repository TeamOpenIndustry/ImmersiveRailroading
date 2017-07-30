package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class TankContainerGui extends GuiContainer {
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;
	private int horizSlots;
    
    public static final int slotSize = 18;
    public static final int topOffset = 17;
    public static final int bottomOffset = 7;
    public static final int textureHeight = 222;
    public static final int paddingRight = 7;
    public static final int paddingLeft = 7;

    public TankContainerGui(CarTank stock, TankContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.horizSlots = 2;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int playerXSize = paddingRight + 9 * slotSize + paddingLeft;
        
        // Top Left Corner
        this.drawTexturedModalRect(i, j, 0, 0, paddingLeft, topOffset);
        // Top Bar
        for (int k = 1; k <= horizSlots; k++) {
        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j, paddingLeft, 0, slotSize, topOffset);
        }
        // Top Right Corner
    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j, paddingLeft + 9 * slotSize, 0, paddingRight, topOffset);
    	
    	for (int l = 0; l < inventoryRows; l++) {
	    	// Left Side
	        this.drawTexturedModalRect(i, j + topOffset + l * slotSize, 0, topOffset, paddingLeft, slotSize);
	        // Middle Slots
	        for (int k = 1; k <= horizSlots; k++) {
	        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j + topOffset + l * slotSize, paddingLeft, topOffset, slotSize, slotSize);
	        }
	        // Right Side
	    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j + topOffset + l * slotSize, paddingLeft + 9 * slotSize, topOffset, paddingRight, slotSize);
    	}
    	if (horizSlots > 9) {
	    	// Left Bottom
	        this.drawTexturedModalRect(i, j + topOffset + inventoryRows * slotSize, 0, textureHeight - bottomOffset, paddingLeft, bottomOffset);
	        // Middle Bottom
	        for (int k = 1; k <= horizSlots; k++) {
	        	this.drawTexturedModalRect(i + paddingLeft + (k-1) * slotSize, j + topOffset + inventoryRows * slotSize, paddingLeft, textureHeight - bottomOffset, slotSize, bottomOffset);
	        }
	        // Right Bottom
	    	this.drawTexturedModalRect(i + paddingLeft + horizSlots * slotSize, j + topOffset + inventoryRows * slotSize, paddingLeft + 9 * slotSize, textureHeight - bottomOffset, paddingRight, bottomOffset);
    	} else if (horizSlots < 9){
            this.drawTexturedModalRect((this.width - playerXSize) / 2, j + topOffset + inventoryRows * slotSize, 0, 0, playerXSize, bottomOffset);
    	} else {
    		this.drawTexturedModalRect((this.width - playerXSize) / 2, j + topOffset + this.inventoryRows * slotSize, 0, 4, playerXSize, 4);
    	}
        
        this.drawTexturedModalRect((this.width - playerXSize) / 2, j + this.inventoryRows * 18 + 17+4, 0, 126+4, playerXSize, 96);
    }
}