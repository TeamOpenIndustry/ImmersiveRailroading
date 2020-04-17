package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;

import java.util.List;

public class ItemTrackExchanger extends ItemBase {
	public ItemTrackExchanger() {
		super(ImmersiveRailroading.MODID, "item_track_exchanger", 1, ItemTabs.MAIN_TAB);
		Fuzzy largeWrench = new Fuzzy("item_large_wrench").add(new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1));
		Fuzzy trackBlueprint = new Fuzzy("item_rail").add(new ItemStack(IRItems.ITEM_TRACK_BLUEPRINT, 1));
		Recipes.register(this, 3,
				Fuzzy.GLASS_PANE, Fuzzy.GLASS_PANE, Fuzzy.GLASS_PANE,
				largeWrench, Fuzzy.IRON_INGOT, trackBlueprint,
				Fuzzy.GLASS_PANE, Fuzzy.REDSTONE_DUST, Fuzzy.GLASS_PANE);
	}

	@Override
	public List<String> getTooltip(ItemStack stack) {
		return CollectionUtil.listOf(GuiText.TRACK_SWITCHER_TOOLTIP.toString());
	}

	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (world.isClient && hand == Hand.PRIMARY) {
			GuiTypes.TRACK_EXCHANGER.open(player);
		}
	}

	public static class Data extends ItemData {
		@TagField("track")
		public String track;

		public Data(ItemStack stack) {
			super(stack);
			if (track == null) {
				track = DefinitionManager.getTracks().get(0).name;
			}
		}
	}
}
