package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class FreightContainerGui extends ContainerGuiBase {
	
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
	private int inventoryRows;
	private int horizSlots;
	private int numSlots;

    public FreightContainerGui(CarFreight stock, FreightContainer container) {
        super(container);
        this.inventoryRows = container.numRows;
        this.horizSlots = stock.getInventoryWidth();
        this.numSlots = container.numSlots;
        this.xSize = paddingRight + horizSlots * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	
    	int currY = j;
        
    	currY = drawTopBar(i, currY, horizSlots);
    	currY = drawSlotBlock(i, currY, horizSlots, inventoryRows, numSlots);
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    }
}