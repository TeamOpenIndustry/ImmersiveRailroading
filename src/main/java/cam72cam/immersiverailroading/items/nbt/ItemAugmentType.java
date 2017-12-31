package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.Augment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemAugmentType {
	public static void set(ItemStack stack, Augment augment) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("augment", augment.ordinal());
	}
	
	public static Augment get(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return Augment.values()[stack.getTagCompound().getInteger("augment")];
		}
		return Augment.WATER_TROUGH;
	}
}
