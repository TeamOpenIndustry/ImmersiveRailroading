package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.FreightTank;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TankContainer extends ContainerBase {
	
	private FreightTank Tank;
	protected int numRows;

	public TankContainer(IInventory playerInventory, FreightTank stock) {
		this.Tank = stock;
        int horizSlots = 10;
		this.numRows = 4;
		
		IItemHandler itemHandler = this.Tank.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

		stock.addListener(this);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = offsetSlotBlock(0, currY, horizSlots, numRows);
		
		this.addSlotToContainer(new FilteredSlot(itemHandler, 0, 0 + paddingLeft + 5, currY - numRows * slotSize + 4, FilteredSlot.FLUID_CONTAINER));
		this.addSlotToContainer(new FilteredSlot(itemHandler, 1, 0 + paddingLeft + slotSize * horizSlots - slotSize - 5, currY - numRows * slotSize + 4, FilteredSlot.NONE));
		
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}
	
	@Override
	public int numSlots() {
		return Tank.getInventorySize();
	}
}
