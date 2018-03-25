package cam72cam.immersiverailroading.gui;

import java.util.Map;

import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class SteamLocomotiveContainerGui extends ContainerGuiBase {
	
	private int inventoryRows;
	private int horizSlots;
	private LocomotiveSteam stock;
	private ItemStack template;

    public SteamLocomotiveContainerGui(LocomotiveSteam stock, SteamLocomotiveContainer container) {
        super(container);
        this.stock = stock;
        this.inventoryRows = container.numRows;
        this.horizSlots = stock.getInventoryWidth();
        this.xSize = paddingRight + horizSlots*2 * slotSize + paddingLeft;
        this.ySize = 114 + this.inventoryRows * slotSize*2;
        this.template = new ItemStack(Items.WATER_BUCKET);
    }

	@Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
    	int currY = j;
    	
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        
        currY = drawTopBar(i, currY, horizSlots*2);
    	currY = drawSlotBlock(i, currY, horizSlots*2, inventoryRows, horizSlots*2 * inventoryRows);
    	
    	drawTankBlock(i + paddingLeft, currY - inventoryRows * slotSize, horizSlots*2, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float)stock.getTankCapacity().MilliBuckets());

    	int quantX = i + paddingLeft + horizSlots*2 * slotSize/2;
    	int quantY = currY - inventoryRows * slotSize + inventoryRows * slotSize/2 - 4;
    	
    	drawSlot(i + paddingLeft+5, currY - inventoryRows * slotSize + 4);
    	drawSlotOverlay(template, i + paddingLeft+5, currY - inventoryRows * slotSize + 4);
    	drawSlot(i + paddingLeft + slotSize * horizSlots*2 - slotSize-5, currY - inventoryRows * slotSize + 4);
    	
    	currY = drawBottomBar(i, currY, horizSlots*2);

    	int prevY = currY;
    	currY = drawSlotBlock(i + horizSlots * slotSize/2, currY, horizSlots, inventoryRows, stock.getInventorySize()-2);
    	try {
    		Map<Integer, Integer> burnTime = stock.getBurnTime();
    		Map<Integer, Integer> burnMax = stock.getBurnMax();
	    	for (int slot : burnTime.keySet()) {
	    		int time = stock.getBurnTime().get(slot);
	    		if (time != 0) {
	    			float perc = Math.min(1f, (float)time / burnMax.get(slot));
	    			
	    			int xSlot = slot % this.horizSlots+1;
	    			int ySlot = slot / this.horizSlots;
	    			
	    			int xPos = i + horizSlots * slotSize/2 + (paddingLeft + (xSlot-1) * slotSize);
	    			int yPos = (prevY) + ySlot * slotSize;
	    			
	    			int offset = 1;
	    			int zOff = (int) ((slotSize-offset*2-1)*(1-perc));
	    			
	    			drawRect(xPos+offset, yPos+offset + zOff, xPos + slotSize-offset, yPos + slotSize-offset, 0x77c64306);
	    			TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
	    			drawSprite(sprite, 0xFFFFFFFF, xPos + offset, yPos + offset + zOff, slotSize-offset*2, slotSize-offset*2 - zOff, 1);
	    		}
	    	}
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    	GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    	
    	currY = drawPlayerInventoryConnector(i, currY, width, horizSlots);
    	currY = drawPlayerInventory((width - playerXSize) / 2, currY);
    	
    	String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
		this.drawCenteredString(this.fontRenderer, quantityStr, quantX, quantY, 14737632);
    }
}