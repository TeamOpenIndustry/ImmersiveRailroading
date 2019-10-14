package cam72cam.immersiverailroading.items.nbt;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.TagCompound;

public class ItemTrackExchanger {
	public static void set(ItemStack stack, String track) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new TagCompound());
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
