package cam72cam.mod.item;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface IInventory {
    int getSlotCount();

    ItemStack get(int slot);
    void set(int slot, ItemStack itemStack);

    ItemStack insert(int slot, ItemStack itemStack, boolean simulate);
    ItemStack extract(int slot, int amount, boolean simulate);

    int getLimit(int slot);

    default void transferAllTo(IInventory to) {
        for (int fromSlot = 0; fromSlot < this.getSlotCount(); fromSlot++) {
            ItemStack stack = this.get(fromSlot);
            int origCount = stack.getCount();

            if (stack.isEmpty()) {
                continue;
            }

            for (int toSlot = 0; toSlot < to.getSlotCount(); toSlot++) {
                stack = to.insert(toSlot, stack, false);
                if (stack.isEmpty()) {
                    break;
                }
            }

            if (origCount != stack.getCount()) {
                this.set(fromSlot, stack);
            }
        }
    }

    default void transferAllFrom(IInventory from) {
        from.transferAllTo(this);
    }

    static IInventory from(IItemHandlerModifiable inv) {
        return new IInventory() {
            @Override
            public int getSlotCount() {
                return inv.getSlots();
            }

            @Override
            public ItemStack get(int slot) {
                return new ItemStack(inv.getStackInSlot(slot));
            }

            @Override
            public void set(int slot, ItemStack itemStack) {
                inv.setStackInSlot(slot, itemStack.internal);
            }

            @Override
            public ItemStack insert(int slot, ItemStack itemStack, boolean simulate) {
                return new ItemStack(inv.insertItem(slot, itemStack.internal, simulate));
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate) {
                return new ItemStack(inv.extractItem(slot, amount, simulate));
            }

            @Override
            public int getLimit(int slot) {
                return inv.getSlotLimit(slot);
            }

        };
    }

    static IInventory from(net.minecraft.inventory.IInventory inventory) {
        return new IInventory() {
            @Override
            public int getSlotCount() {
                return inventory.getSizeInventory();
            }

            @Override
            public ItemStack get(int slot) {
                return new ItemStack(inventory.getStackInSlot(slot));
            }

            @Override
            public void set(int slot, ItemStack itemStack) {
                inventory.setInventorySlotContents(slot, itemStack.internal);
            }

            @Override
            public ItemStack insert(int slot, ItemStack itemStack, boolean simulate) {
                net.minecraft.item.ItemStack current = inventory.getStackInSlot(slot);

                if (current.isEmpty()) {
                    set(slot, itemStack);
                    return ItemStack.EMPTY;
                }

                if (itemStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                if (!itemStack.internal.isItemEqual(current)) {
                    return itemStack;
                }
                if (!net.minecraft.item.ItemStack.areItemStackTagsEqual(itemStack.internal, current)) {
                    return itemStack;
                }

                int space = current.getMaxStackSize() - current.getCount();
                if (space >= 0) {
                    return itemStack;
                }

                int toMove = Math.min(space, itemStack.getCount());
                if (!simulate) {
                    ItemStack copy = itemStack.copy();
                    copy.setCount(toMove);
                    set(slot, copy);
                }

                ItemStack remainder = new ItemStack(itemStack.internal);
                remainder.setCount(itemStack.getCount() - toMove);
                return remainder;
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate) {
                net.minecraft.item.ItemStack backup = inventory.getStackInSlot(slot).copy();
                net.minecraft.item.ItemStack output = inventory.decrStackSize(slot, amount);
                if (simulate) {
                    inventory.setInventorySlotContents(slot, backup);
                }
                return new ItemStack(output);
            }

            @Override
            public int getLimit(int slot) {
                return 0;
            }
        };
    }
}
