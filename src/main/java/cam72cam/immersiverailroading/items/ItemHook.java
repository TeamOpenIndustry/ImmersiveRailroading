package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.util.CollectionUtil;

import java.util.List;

public class ItemHook extends ItemBase {
	public ItemHook() {
		super(ImmersiveRailroading.MODID, "item_hook");


		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 2,
				steel, steel, steel, null, steel, null);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return CollectionUtil.listOf(ItemTabs.MAIN_TAB);
	}

}
