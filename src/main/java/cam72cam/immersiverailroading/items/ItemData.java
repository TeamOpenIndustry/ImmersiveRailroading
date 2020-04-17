package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;

public abstract class ItemData {
    private final ItemStack stack;

    protected ItemData(ItemStack stack) {
        this.stack = stack;
        try {
            TagSerializer.deserialize(stack.getTagCompound(), this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
    }

    public void write() {
        try {
            TagSerializer.serialize(stack.getTagCompound(), this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
    }
}