package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class SteamLocomotiveContainer extends ContainerBase {
	
	private LocomotiveSteam Tank;
	protected int numRows;

	public SteamLocomotiveContainer(IInventory playerInventory, LocomotiveSteam stock) {
		this.Tank = stock;
        int horizSlots = stock.getInventoryWidth();
		this.numRows = (int) Math.ceil(((double)stock.getInventorySize()-2) / horizSlots);
		
		IItemHandler itemHandler = this.Tank.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

		stock.addListener(this);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots*2);
		currY = offsetSlotBlock(0, currY, horizSlots*2, numRows);
		
		this.addSlotToContainer(new FilteredSlot(itemHandler, stock.getInventorySize()-2, 0 + paddingLeft + 5, currY - numRows * slotSize + 4, FilteredSlot.FLUID_CONTAINER));
		this.addSlotToContainer(new FilteredSlot(itemHandler, stock.getInventorySize()-1, 0 + paddingLeft + slotSize * horizSlots*2 - slotSize - 5, currY - numRows * slotSize + 4, FilteredSlot.NONE));
		currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots*2);
		
		currY = addFilteredSlotBlock(itemHandler, stock.getInventorySize()-2, horizSlots * slotSize/2, currY, horizSlots, FilteredSlot.BURNABLE);
		
    	currY = offsetPlayerInventoryConnector(0, currY, width/2, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots*2);
	}
	
	@Override
	public int numSlots() {
		return Tank.getInventorySize();
	}
}
