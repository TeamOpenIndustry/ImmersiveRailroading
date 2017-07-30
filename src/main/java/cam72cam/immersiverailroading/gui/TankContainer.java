package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TankContainer extends ContainerBase {
	
	private CarTank Tank;
	protected int numRows;

	public TankContainer(IInventory playerInventory, CarTank stock) {
		this.Tank = stock;
        int horizSlots = 10;
		this.numRows = 4;
		
		IItemHandler itemHandler = this.Tank.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

		stock.addListener(this);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = offsetSlotBlock(0, currY, horizSlots, numRows);
		
		this.addSlotToContainer(new SlotItemHandler(itemHandler, 0, 0 + paddingLeft + 5, currY - numRows * slotSize + (int)(slotSize * 1.5)));
		this.addSlotToContainer(new SlotItemHandler(itemHandler, 1, 0 + paddingLeft + slotSize * horizSlots - slotSize - 5, currY - numRows * slotSize + (int)(slotSize * 1.5)));
		
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

}
