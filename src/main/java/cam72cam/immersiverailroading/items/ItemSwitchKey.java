package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;

import java.util.List;

public class ItemSwitchKey extends ItemBase {
	public ItemSwitchKey() {
		super(ImmersiveRailroading.MODID, "item_switch_key", 1, ItemTabs.MAIN_TAB);
	}

	@Override
	public void addInformation(ItemStack stack, List<String> tooltip)
	{
		tooltip.add(GuiText.SWITCH_HAMMER_TOOLTIP.toString());
	}
}
