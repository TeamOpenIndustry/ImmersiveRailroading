package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.Recipes;

public class ItemHook extends ItemBase {
	public ItemHook() {
		super(ImmersiveRailroading.MODID, "item_hook", 1, ItemTabs.MAIN_TAB);


		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 2,
				steel, steel, steel, null, steel, null);
	}
}
