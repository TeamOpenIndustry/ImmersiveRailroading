package cam72cam.immersiverailroading.items.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemTrackExchanger {
	public static void set(ItemStack stack, String track) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("track", track);
	}
	
	public static String get(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getString("track");
		}
		return null;
	}
}
