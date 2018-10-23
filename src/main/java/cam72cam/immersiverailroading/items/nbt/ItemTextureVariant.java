package cam72cam.immersiverailroading.items.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemTextureVariant {
	public static void set(ItemStack stack, String texture) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		if (texture == null) {
			stack.getTagCompound().removeTag("texture_variant");
		} else {
			stack.getTagCompound().setString("texture_variant", texture);			
		}
	}
	
	public static String get(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("texture_variant")) {
			return stack.getTagCompound().getString("texture_variant");
		}
		return null;
	}
}
