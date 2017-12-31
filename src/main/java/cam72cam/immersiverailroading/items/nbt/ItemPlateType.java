package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.PlateType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemPlateType {
	public static void set(ItemStack stack, PlateType plate) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("plate", plate.ordinal());
	}
	
	public static PlateType get(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("plate")){
			return PlateType.values()[stack.getTagCompound().getInteger("plate")];
		}
		return PlateType.SMALL;
	}
}
