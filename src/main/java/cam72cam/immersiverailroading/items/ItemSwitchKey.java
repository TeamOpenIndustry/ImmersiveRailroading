package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.util.CollectionUtil;

import java.util.List;

public class ItemSwitchKey extends ItemBase {
	public ItemSwitchKey() {
		super(ImmersiveRailroading.MODID, "item_switch_key", 1, ItemTabs.MAIN_TAB);

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 2,
				null, steel, null, steel, steel, steel);
	}

	@Override
	public List<String> getTooltip(ItemStack stack)
	{
		return CollectionUtil.listOf(GuiText.SWITCH_HAMMER_TOOLTIP.toString());
	}
}
