package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.ItemComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemComponent {
	public static ItemComponentType getComponentType(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return ItemComponentType.values()[stack.getTagCompound().getInteger("componentType")];
		}
		stack.setCount(0);
		return ItemComponentType.values()[0];
	}
	public static void setComponentType(ItemStack stack, ItemComponentType item) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("componentType", item.ordinal());
	}
}
