package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.PlateType;
import cam72cam.mod.item.ItemStack;

public class ItemPlateType {
	public static void set(ItemStack stack, PlateType plate) {
		stack.getTagCompound().setInteger("plate", plate.ordinal());
	}
	
	public static PlateType get(ItemStack stack) {
		if (stack.getTagCompound().hasKey("plate")){
			return PlateType.values()[stack.getTagCompound().getInteger("plate")];
		}
		return PlateType.SMALL;
	}
}
