package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.FreightTank;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class TankContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private FreightTank stock;
	private ItemStack template;

    public TankContainerGui(FreightTank stock, TankContainer container) {
        super(container);
        this.stock = stock;
        this.inventoryRows = container.numRows;
        this.horizSlots = 10;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
        this.template = new ItemStack(Items.BUCKET);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int currY = j;
    	
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows, horizSlots * inventoryRows);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float)stock.getTankCapacity().MilliBuckets());
    	int quantX = i + paddingLeft + horizSlots * slotSize/2;
    	int quantY = currY - inventoryRows * slotSize + inventoryRows * slotSize/2;

    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + 4);
    	drawSlotOverlay(template, i + paddingLeft+5, currY - inventoryRows * slotSize + 4);
		
    	drawSlot(i + paddingLeft + slotSize * horizSlots - slotSize-5, currY - inventoryRows * slotSize + 4);
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    	

		String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
		this.drawCenteredString(this.fontRenderer, quantityStr, quantX, quantY, 14737632);
    }
}