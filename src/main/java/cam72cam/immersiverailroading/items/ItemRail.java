package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.CollectionUtil;

public class ItemRail extends ItemBase {
	public ItemRail() {
		super(ImmersiveRailroading.MODID, "item_rail_part", 64, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public List<String> getTooltip(ItemStack stack) {
        return CollectionUtil.listOf(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
    }
}
