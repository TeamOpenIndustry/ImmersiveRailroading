package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemDefinition {
	public static void setID(ItemStack stack, String def) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("defID", def);
	}
	
	public static String getID(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getString("defID");
		}
		stack.setCount(0);
		return "";
	}
	
	public static EntityRollingStockDefinition get(ItemStack stack) {
		return DefinitionManager.getDefinition(getID(stack));
	}
	
}
