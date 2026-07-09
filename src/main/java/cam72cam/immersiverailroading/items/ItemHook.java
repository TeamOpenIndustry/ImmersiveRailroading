package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.umc.api.item.CreativeTab;
import cam72cam.umc.api.item.Fuzzy;
import cam72cam.umc.api.item.CustomItem;

import java.util.Collections;
import java.util.List;

public class ItemHook extends CustomItem {
	public ItemHook() {
		super(ImmersiveRailroading.MODID, "item_hook");


		Fuzzy steel = Fuzzy.STEEL_INGOT;
		IRFuzzy.registerSteelRecipe(this, 2,
				steel, steel,
				steel, null,
				steel, null);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}

}
