package cam72cam.mod.item;

import cam72cam.mod.util.TagCompound;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.item.ItemStack.EMPTY);

    public net.minecraft.item.ItemStack internal;
    public final Item item;

    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
        this.item = internal.getItem();
    }

    public ItemStack(Item item, int count) {
        this(new net.minecraft.item.ItemStack(item, count));
    }

    public ItemStack(TagCompound bedItem) {
        this(new net.minecraft.item.ItemStack(bedItem.internal));
    }

    public ItemStack(Item item, int count, int meta) {
        this(new net.minecraft.item.ItemStack(item, count, meta));
    }

    public ItemStack(Block block) {
        this(new net.minecraft.item.ItemStack(block));
    }

    public ItemStack(Block block, int count, int meta) {
        this(new net.minecraft.item.ItemStack(block, count, meta));
    }

    public ItemStack(Item item) {
        this(new net.minecraft.item.ItemStack(item));
    }

    public TagCompound getTagCompound() {
        if (internal.getTagCompound() == null) {
            internal.setTagCompound(new TagCompound().internal);
        }
        return new TagCompound(internal.getTagCompound());
    }
    public void setTagCompound(TagCompound data) {
        internal.setTagCompound(data.internal);
    }

    public ItemStack copy() {
        return new ItemStack(internal.copy());
    }

    public TagCompound toTag() {
        return new TagCompound(internal.serializeNBT());
    }

    public int getCount() {
        return internal.getCount();
    }
    public void setCount(int count) {
        internal.setCount(count);
    }

    public String getDisplayName() {
        return internal.getDisplayName();
    }

    public boolean isEmpty() {
        return internal.isEmpty();
    }

    public void shrink(int i) {
        internal.shrink(i);
    }

    public boolean equals(ItemStack other) {
        return internal.isItemEqual(other.internal);
    }
}
