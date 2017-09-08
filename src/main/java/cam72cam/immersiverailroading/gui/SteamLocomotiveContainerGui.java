package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import net.minecraft.client.renderer.GlStateManager;

public class SteamLocomotiveContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private LocomotiveSteam stock;

    public SteamLocomotiveContainerGui(LocomotiveSteam stock, SteamLocomotiveContainer container) {
        super(container);
        this.stock = stock;
        this.inventoryRows = container.numRows;
        this.horizSlots = stock.getInventoryWidth();
        this.xSize = paddingRight + horizSlots*2 * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }

	@Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int currY = j;
    	
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots*2);
    	currY = drawSlotBlock(i, currY, horizSlots*2, inventoryRows);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots*2, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float)stock.getTankCapacity().MilliBuckets());
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	drawSlot(i + paddingLeft + slotSize * horizSlots*2 - slotSize-5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	
    	currY = drawBottomBar(i, currY, horizSlots*2);

    	currY = drawSlotBlock(i + horizSlots * slotSize/2, currY, horizSlots, inventoryRows);
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}