package cam72cam.mod.item;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface IInventory {
    int getSlotCount();

    ItemStack get(int slot);
    void set(int slot, ItemStack itemStack);

    ItemStack insert(int slot, ItemStack itemStack, boolean simulate);
    ItemStack extract(int slot, int amount, boolean simulate);

    int getLimit(int slot);

    // This is kinda lazy but might be efficient?
    IItemHandlerModifiable internal();

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

            @Override
            public IItemHandlerModifiable internal() {
                return inv;
            }
        };
    }
}
