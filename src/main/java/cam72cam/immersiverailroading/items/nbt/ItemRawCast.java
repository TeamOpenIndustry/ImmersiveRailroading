package cam72cam.immersiverailroading.items.nbt;

import cam72cam.mod.item.ItemStack;

public class ItemRawCast {
	public static void set(ItemStack stack, boolean isRaw) {
		stack.getTagCompound().setBoolean("raw_cast", isRaw);
	}
	
	public static boolean get(ItemStack stack) {
        return stack.getTagCompound().getBoolean("raw_cast");
	}
}
