package cam72cam.immersiverailroading.items.nbt;

import java.util.List;

import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.item.ItemStack;

public class ItemMultiblockType {
	public static void set(ItemStack stack, String name) {
		stack.getTagCompound().setString("name", name);
	}
	
	public static String get(ItemStack stack) {
		List<String> keys = MultiblockRegistry.keys();
        String name = stack.getTagCompound().getString("name");
        if (keys.contains(name)) {
            return name;
        }
		if (keys.size() == 0) {
			return "";
		}
		return keys.get(0);
	}
}
