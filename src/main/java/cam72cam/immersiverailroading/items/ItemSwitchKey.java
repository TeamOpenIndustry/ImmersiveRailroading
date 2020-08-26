package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.item.*;
import cam72cam.mod.util.CollectionUtil;

import java.util.List;

public class ItemSwitchKey extends CustomItem {
	public ItemSwitchKey() {
		super(ImmersiveRailroading.MODID, "item_switch_key");

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 2,
				null, steel, null, steel, steel, steel);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return CollectionUtil.listOf(ItemTabs.MAIN_TAB);
	}

	@Override
	public List<String> getTooltip(ItemStack stack)
	{
		return CollectionUtil.listOf(GuiText.SWITCH_HAMMER_TOOLTIP.toString());
	}
}
