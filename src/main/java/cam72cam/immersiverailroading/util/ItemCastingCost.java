package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemCastRail;
import cam72cam.immersiverailroading.items.ItemRailAugment;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

public class ItemCastingCost {
	public static final int BAD_CAST_COST = -999;
	
	public static int getCastCost(ItemStack item) {
		int cost = BAD_CAST_COST;
		int count = 1;
		if (item.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
			ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
			cost = data.componentType.getCastCost(data.def, data.gauge);
		} else if (item.is(IRItems.ITEM_CAST_RAIL)) {
			cost = (int) Math.ceil(20 * new ItemCastRail.Data(item).gauge.scale());
		} else if (item.is(IRItems.ITEM_AUGMENT)) {
			cost = (int) Math.ceil(8 * new ItemRailAugment.Data(item).gauge.scale());
			count = 8;
		} else if (IRFuzzy.steelBlockOrFallback().matches(item)) {
			cost = 9;
		} else if (IRFuzzy.steelIngotOrFallback().matches(item)) {
			cost = 1;
		}
		item.setCount(count);
		return cost;
	}
}
