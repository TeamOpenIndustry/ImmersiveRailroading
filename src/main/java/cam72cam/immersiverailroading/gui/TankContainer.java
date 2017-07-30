package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TankContainer extends ContainerBase {
	
	private CarTank Tank;
	protected int numRows;

	public TankContainer(IInventory playerInventory, CarTank stock) {
		this.Tank = stock;
        int horizSlots = 2;
		this.numRows = (int) Math.ceil((double)stock.getInventorySize() / horizSlots);
		
		IItemHandler itemHandler = this.Tank.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

		stock.addListener(this);
		
		int width = 0;
		int currY = 0;
		currY = offsetTopBar(0, currY, horizSlots);
		currY = addSlotBlock(itemHandler, stock.getInventorySize(), 0, currY, horizSlots);
    	currY = offsetPlayerInventoryConnector(0, currY, width, horizSlots);
    	currY = addPlayerInventory(playerInventory, currY, horizSlots);
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}

}
