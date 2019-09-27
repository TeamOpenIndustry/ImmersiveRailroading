package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.mod.item.ItemStack;

public class ItemComponent {
	public static ItemComponentType getComponentType(ItemStack stack) {
        return ItemComponentType.values()[stack.getTagCompound().getInteger("componentType")];
	}
	public static void setComponentType(ItemStack stack, ItemComponentType item) {
		stack.getTagCompound().setInteger("componentType", item.ordinal());
	}
}
