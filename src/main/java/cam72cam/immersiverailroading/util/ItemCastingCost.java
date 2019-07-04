package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import net.minecraft.item.ItemStack;

public class ItemCastingCost {
	public static final int BAD_CAST_COST = -999;
	
	public static int getCastCost(ItemStack item) {
		int cost = BAD_CAST_COST;
		int count = 1;
		if (item.getItem() == IRItems.ITEM_ROLLING_STOCK_COMPONENT) {
			ItemComponentType component = ItemComponent.getComponentType(item);
			cost = component.getCastCost(ItemDefinition.get(item), ItemGauge.get(item));
		} else if (item.getItem() == IRItems.ITEM_CAST_RAIL) {
			cost = (int) Math.ceil(20 * ItemGauge.get(item).scale());
		} else if (item.getItem() == IRItems.ITEM_AUGMENT) {
			cost = (int) Math.ceil(8 * ItemGauge.get(item).scale());
			count = 8;
		} else if (OreHelper.IR_STEEL_BLOCK.matches(item, false)) {
			cost = 9;
		} else if (OreHelper.IR_STEEL_INGOT.matches(item, false)) {
			cost = 1;
		}
		item.setCount(count);
		return cost;
	}
}
