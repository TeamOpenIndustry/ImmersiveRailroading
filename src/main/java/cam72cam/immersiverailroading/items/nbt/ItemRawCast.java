package cam72cam.immersiverailroading.items.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemRawCast {
	public static void set(ItemStack stack, boolean isRaw) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setBoolean("raw_cast", isRaw);
	}
	
	public static boolean get(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getBoolean("raw_cast");
		}
		return true;
	}
}
