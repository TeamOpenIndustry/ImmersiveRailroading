package cam72cam.immersiverailroading.items.nbt;

import java.util.List;

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
		List<String> keys = MultiblockRegistry.keys();
		if (stack.getTagCompound() != null) {
			String name = stack.getTagCompound().getString("name");
			if (keys.contains(name)) {
				return name;
			}
		}
		if (keys.size() == 0) {
			return "";
		}
		return keys.get(0);
	}
}
