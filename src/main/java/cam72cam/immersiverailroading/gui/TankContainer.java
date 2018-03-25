package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.FreightTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
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
            if (index <= 2) {
            	if (!this.mergeItemStack(itemstack1, 2, this.inventorySlots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
            	if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
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
