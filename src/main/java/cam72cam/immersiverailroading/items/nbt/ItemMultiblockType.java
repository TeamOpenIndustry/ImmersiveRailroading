package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemMultiblockType {
	public static void set(ItemStack stack, String name) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("name", name);
	}
	
	public static String get(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getString("name");
		}
		if (MultiblockRegistry.keys().size() == 0) {
			return "";
		}
		return MultiblockRegistry.keys().get(0);
	}
}
