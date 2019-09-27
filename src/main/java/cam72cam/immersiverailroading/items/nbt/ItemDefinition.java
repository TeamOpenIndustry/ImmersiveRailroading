package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.item.ItemStack;

public class ItemDefinition {
	public static void setID(ItemStack stack, String def) {
		stack.getTagCompound().setString("defID", def);
	}
	
	public static String getID(ItemStack stack) {
        return stack.getTagCompound().getString("defID");
	}
	
	public static EntityRollingStockDefinition get(ItemStack stack) {
		return DefinitionManager.getDefinition(getID(stack));
	}
}
