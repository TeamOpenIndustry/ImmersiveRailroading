package cam72cam.mod.capability;

import cam72cam.mod.item.ItemStack;

public interface IInventory {
    int getSlotCount();

    ItemStack get(int slot);
    void set(int slot, ItemStack itemStack);

    ItemStack insert(int slot, ItemStack itemStack, boolean simulate);
    ItemStack extract(int slot, int amount, boolean simulate);

    int getLimit(int slot);
}
