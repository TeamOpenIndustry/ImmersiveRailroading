package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.Tender;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TenderContainer extends ContainerBase {
	
	private Tender Tank;
	protected int numRows;

	public TenderContainer(IInventory playerInventory, Tender stock) {
		this.Tank = stock;
        int horizSlots = stock.getInventoryWidth();
		this.numRows = (int) Math.ceil(((double)stock.getInventorySize()-2) / horizSlots);
		
		IItemHandler itemHandler = this.Tank.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

		stock.addListener(this);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = offsetSlotBlock(0, currY, horizSlots, numRows);
		
		this.addSlotToContainer(new SlotItemHandler(itemHandler, stock.getInventorySize()-2, 0 + paddingLeft + 5, currY - numRows * slotSize + 4));
		this.addSlotToContainer(new SlotItemHandler(itemHandler, stock.getInventorySize()-1, 0 + paddingLeft + slotSize * horizSlots - slotSize - 5, currY - numRows * slotSize + 4));
		
		currY = addSlotBlock(itemHandler, stock.getInventorySize()-2, 0, currY, horizSlots);
		
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}
	
	@Override
	public int numSlots() {
		return Tank.getInventorySize();
	}
}
