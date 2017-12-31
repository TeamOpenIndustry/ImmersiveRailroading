package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.Gauge;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemGauge {	
	public static void set(ItemStack stack, Gauge gauge) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setDouble("gauge", gauge.value());
	}
	
	public static Gauge get(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("gauge")){
			return Gauge.from(stack.getTagCompound().getDouble("gauge"));
		}
		return Gauge.STANDARD;
	}
}
