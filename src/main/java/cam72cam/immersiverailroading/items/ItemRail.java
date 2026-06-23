package cam72cam.immersiverailroading.items;

import java.util.Collections;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.umc.api.item.CreativeTab;
import cam72cam.umc.api.item.CustomItem;
import cam72cam.umc.api.item.ItemStack;
import cam72cam.umc.api.serialization.TagField;
import cam72cam.umc.api.text.TextUtil;

public class ItemRail extends CustomItem {
	public ItemRail() {
		super(ImmersiveRailroading.MODID, "item_rail_part");
	}

	@Override
	public int getStackSize() {
		return 64;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}

	public String getCustomName(ItemStack stack) {
		return String.format("%s (%s)", TextUtil.translate("item.immersiverailroading:item_rail_part.name"), new Data(stack).gauge.toString());
	}

	public static class Data extends ItemDataSerializer {
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
