package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.CollectionUtil;

public class ItemCastRail extends ItemBase {
	public ItemCastRail() {
		super(ImmersiveRailroading.MODID, "item_cast_rail");
	}

    @Override
    public int getStackSize() {
        return 16;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return CollectionUtil.listOf(ItemTabs.MAIN_TAB);
    }

    @Override
    public List<String> getTooltip(ItemStack stack)
    {
        return CollectionUtil.listOf(GuiText.GAUGE_TOOLTIP.toString(new Data(stack).gauge));
    }

    public static class Data extends ItemData {
	    @TagField("gauge")
        public Gauge gauge;

        public Data(ItemStack stack) {
            super(stack);
            if (gauge == null) {
                gauge = Gauge.from(Gauge.STANDARD);
            }
        }
    }
}
