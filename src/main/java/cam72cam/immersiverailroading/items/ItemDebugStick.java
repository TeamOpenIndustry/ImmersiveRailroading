package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;

import java.util.Collections;
import java.util.List;

public class ItemDebugStick extends CustomItem {

	public ItemDebugStick() {
		super(ImmersiveRailroading.MODID, "item_debug_stick");
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
