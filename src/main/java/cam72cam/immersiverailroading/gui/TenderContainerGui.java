package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.Tender;
import net.minecraft.client.renderer.GlStateManager;

public class TenderContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private Tender stock;

    public TenderContainerGui(Tender stock, TenderContainer container) {
        super(container);
        this.stock = stock;
        this.inventoryRows = container.numRows;
        this.horizSlots = stock.getInventoryWidth();
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
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
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots, inventoryRows, stock.getClientLiquid(), stock.getClientLiquidAmount() / (float)stock.getTankCapacity());
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));
    	drawSlot(i + paddingLeft + slotSize * horizSlots - slotSize-5, currY - inventoryRows * slotSize + (int)(slotSize * 1.5));

    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows);
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}