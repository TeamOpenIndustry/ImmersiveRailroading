package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.Augment;
import cam72cam.mod.item.ItemStack;

public class ItemAugmentType {
	public static void set(ItemStack stack, Augment augment) {
		stack.getTagCompound().setInteger("augment", augment.ordinal());
	}
	
	public static Augment get(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return Augment.values()[stack.getTagCompound().getInteger("augment")];
		}
		return Augment.WATER_TROUGH;
	}
}
