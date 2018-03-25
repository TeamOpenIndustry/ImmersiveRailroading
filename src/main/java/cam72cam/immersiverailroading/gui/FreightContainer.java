package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarFreight;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
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
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}
	
	@Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = null;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index <= freight.getInventorySize()) {
            	if (!this.mergeItemStack(itemstack1, freight.getInventorySize(), this.inventorySlots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
            	if (!this.mergeItemStack(itemstack1, 0, freight.getInventorySize()-1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
