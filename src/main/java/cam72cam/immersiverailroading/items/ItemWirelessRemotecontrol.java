package cam72cam.immersiverailroading.items;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.serialization.TagField;

public class ItemWirelessRemotecontrol extends CustomItem {
	
	public ItemWirelessRemotecontrol() {
		super(ImmersiveRailroading.MODID, "item_wireless_remotecontrol");
		
		Fuzzy redstoneTorch = Fuzzy.REDSTONE_TORCH;
		Fuzzy redstone = Fuzzy.REDSTONE_DUST;
		Fuzzy iron = Fuzzy.IRON_INGOT;
		Fuzzy gold = Fuzzy.GOLD_INGOT;
		Recipes.shapedRecipe(this, 3,
				redstone, redstoneTorch, redstone,
				iron, gold, iron,
				iron, iron, iron);
	}

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<String> getTooltip(ItemStack stack) {
        Data d = new Data(stack);
        return Collections.singletonList(d.linked == null ? "Not linked to any locomotive" : "Linked to: " + d.linked);
    }

	public static class Data extends ItemDataSerializer {
		@TagField("linked")
		public UUID linked;

		public Data(ItemStack stack) {
			super(stack);
		}
	}
}
