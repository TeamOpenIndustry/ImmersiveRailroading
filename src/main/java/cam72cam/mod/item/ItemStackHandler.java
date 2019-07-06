package cam72cam.mod.item;

import cam72cam.mod.util.TagCompound;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

public class ItemStackHandler {
    public final net.minecraftforge.items.ItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;

    public ItemStackHandler(int size) {
        this.internal = new net.minecraftforge.items.ItemStackHandler(size) {
            @Override
            public void setStackInSlot(int slot, @Nonnull net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    super.setStackInSlot(slot, stack.copy());
                }
            }

            @Override
            @Nonnull
            public net.minecraft.item.ItemStack insertItem(int slot, @Nonnull net.minecraft.item.ItemStack stack, boolean simulate) {
                return checkSlot.test(slot, new ItemStack(stack)) ? super.insertItem(slot, stack.copy(), simulate) : stack;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                ItemStackHandler.this.onContentsChanged(slot);
            }
        };
    }

    public ItemStackHandler() {
        this(1);
    }

    protected void onContentsChanged(int slot) {
        //NOP
    }

    public void setSize(int inventorySize) {
        internal.setSize(inventorySize);
    }

    public int getSlots() {
        return internal.getSlots();
    }

    public ItemStack getStackInSlot(int slot) {
        return new ItemStack(internal.getStackInSlot(slot));
    }
    public void setStackInSlot(int slot, ItemStack stack) {
        internal.setStackInSlot(slot, stack.internal);
    }
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return new ItemStack(internal.insertItem(slot, stack.internal, simulate));
    }
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return new ItemStack(internal.extractItem(slot, amount, simulate));
    }

    public TagCompound save() {
        return new TagCompound(internal.serializeNBT());
    }

    public void load(TagCompound items) {
        internal.deserializeNBT(items.internal);
    }

}
