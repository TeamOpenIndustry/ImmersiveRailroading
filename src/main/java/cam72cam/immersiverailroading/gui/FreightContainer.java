package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class FreightContainer extends ContainerBase {
	
	private CarFreight freight;
	protected int numRows;
	protected int numSlots;

	public FreightContainer(IInventory playerInventory, CarFreight stock) {
		this.freight = stock;
        int horizSlots = stock.getInventoryWidth();
		this.numRows = (int) Math.ceil((double)stock.getInventorySize() / horizSlots);
		this.numSlots = stock.getInventorySize(); 
		
		IItemHandler itemHandler = this.freight.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = addSlotBlock(itemHandler, stock.getInventorySize(), 0, currY, horizSlots);
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}

	@Override
	public int numSlots() {
		return freight.getInventorySize();
	}
}
