package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
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
		
		EntityRollingStockDefinition def = ItemDefinition.get(stack.copy());
		if (def != null) {
			return def.recommended_gauge;
		}
		return Gauge.from(Gauge.STANDARD);
	}
}
