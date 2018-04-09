package cam72cam.immersiverailroading.gui;

import java.util.function.Function;

import javax.annotation.Nonnull;

import cam72cam.immersiverailroading.util.BurnUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public abstract class ContainerBase extends Container implements ISyncableSlots {

	public static final int slotSize = 18;
    public static final int topOffset = 18;
    public static final int bottomOffset = 7;
    public static final int textureHeight = 222;
    public static final int paddingRight = 7;
    public static final int paddingLeft = 8;
    public static final int stdUiHorizSlots = 9;
    public static final int playerXSize = paddingLeft + stdUiHorizSlots * slotSize + paddingRight;
	private static final int midBarHeight = 4;
	
    public int offsetTopBar(int x, int y, int slots) {
    	return y + topOffset;
    }
    
    public int drawSlotRow(int x, int y, int slots) {
    	return y + slotSize;
    }

	public int offsetSlotBlock(int x, int y, int slotX, int slotY) {
		for (int i = 0; i < slotY; i++) {
			y = drawSlotRow(x, y, slotX);
		}
		return y;
	}

	public int drawBottomBar(int x, int y, int slots) {
    	return y + bottomOffset;
	}

	public int drawPlayerTopBar(int x, int y) {
		return y + bottomOffset;
	}

	public int drawPlayerMidBar(int x, int y) {
		return y + midBarHeight;
	}

	public int offsetPlayerInventory(int x, int y) {
		return y+96;
	}

	public int offsetPlayerInventoryConnector(int x, int y, int aboveWidth, int horizSlots) {
    	if (horizSlots > 9) {
    		return drawBottomBar(x, y, horizSlots);
    	} else if (horizSlots < 9){
    		return drawPlayerTopBar((aboveWidth - playerXSize) / 2, y);
    	} else {
    		return drawPlayerMidBar((aboveWidth - playerXSize) / 2, y);
    	}
	}
	
	public int addSlotBlock(IItemHandler handler, int slots, int x, int y, int horizSlots) {
		for (int slotID = 0; slotID < slots; slotID++) {
			int row = slotID / horizSlots;
			int col = slotID % horizSlots;
            this.addSlotToContainer(new SlotItemHandler(handler, slotID, x + paddingLeft + col * slotSize, y + row * slotSize));
		}
		return y + slotSize * (int) Math.ceil((double)slots / horizSlots);
	}
	
	public int addFilteredSlotBlock(IItemHandler handler, int slots, int x, int y, int horizSlots, Function<ItemStack, Boolean> fn) {
		for (int slotID = 0; slotID < slots; slotID++) {
			int row = slotID / horizSlots;
			int col = slotID % horizSlots;
            this.addSlotToContainer(new FilteredSlot(handler, slotID, x + paddingLeft + col * slotSize, y + row * slotSize, fn));
		}
		return y + slotSize * (int) Math.ceil((double)slots / horizSlots);
	}

	public int addPlayerInventory(IInventory playerInventory, int currY, int horizSlots) {
    	currY += 9;

		int normInvOffset = (horizSlots - stdUiHorizSlots) * slotSize / 2 + paddingLeft;
		
        for (int l = 0; l < 3; ++l)
        {
            for (int j1 = 0; j1 < stdUiHorizSlots; ++j1)
            {
                this.addSlotToContainer(new Slot(playerInventory, j1 + l * stdUiHorizSlots + stdUiHorizSlots, normInvOffset + j1 * slotSize, currY));
            }
    		currY += slotSize;
        }
        currY += 4;
        
		for (int i1 = 0; i1 < 9; ++i1)
        {
            this.addSlotToContainer(new Slot(playerInventory, i1, normInvOffset + i1 * slotSize, currY));
        }
		currY += slotSize;
		
		return currY;
	}
	

	
	/*
	 * Workaround a bug where stacks were not synced correctly
	 */
	@Override
	public void syncSlots()
    {
        for (int i = 0; i < this.inventorySlots.size(); ++i)
        {
            ItemStack itemstack = this.inventorySlots.get(i).getStack();
            itemstack = itemstack.isEmpty() ? ItemStack.EMPTY : itemstack.copy();

            for (int j = 0; j < this.listeners.size(); ++j)
            {
                this.listeners.get(j).sendSlotContents(this, i, itemstack);
            }
        }
    }


	@Override
	public final boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}
	
	public abstract int numSlots();
	
	@Override
    public final ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < numSlots()) {
            	if (!this.mergeItemStack(itemstack1, numSlots(), this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, numSlots(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
	
	/*
	@Override 
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		try {
			return super.slotClick(slotId, dragType, clickTypeIn, player);
		} catch (Exception ex) {
			// This is a crappy hack
			return ItemStack.EMPTY; 
		}
	}*/
	
	public static class FilteredSlot extends SlotItemHandler {
		private Function<ItemStack, Boolean> fn;
		
		public static final Function<ItemStack, Boolean> FLUID_CONTAINER = (ItemStack stack) -> {
			IFluidHandlerItem containerFluidHandler = FluidUtil.getFluidHandler(stack.copy());
			return containerFluidHandler != null;
		};
		
		public static final Function<ItemStack, Boolean> BURNABLE = (ItemStack stack) -> {
			return BurnUtil.getBurnTime(stack) != 0;
		};
		
		public static final Function<ItemStack, Boolean> NONE = (ItemStack stack) -> {
			return false;
		};

		public FilteredSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Function<ItemStack, Boolean> acceptor) {
			super(itemHandler, index, xPosition, yPosition);
			this.fn = acceptor;
		}
		
		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			return fn.apply(stack) && super.isItemValid(stack);
		}
	}
}
