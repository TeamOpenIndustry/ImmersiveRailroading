package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.Recipes;

public class ItemPaintBrush extends ItemBase {
	public ItemPaintBrush() {
		super(ImmersiveRailroading.MODID, "item_paint_brush", 1, ItemTabs.MAIN_TAB);

		Recipes.register(this, 1,
				Fuzzy.WOOL_BLOCK, Fuzzy.IRON_INGOT, Fuzzy.WOOD_STICK);
	}
}
