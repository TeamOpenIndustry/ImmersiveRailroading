package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.text.TextColor;

public class ItemRollingStockComponent extends BaseItemRollingStock {

	public ItemRollingStockComponent() {
		super(ImmersiveRailroading.MODID, "item_rolling_stock_component");
	}

	@Override
	public int getStackSize() {
		return 64;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return CollectionUtil.listOf(ItemTabs.COMPONENT_TAB);
	}

	@Override
	public String getCustomName(ItemStack stack) {
		return super.getCustomName(stack) + " " + new Data(stack).componentType.toString();
	}

	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab) {
		List<ItemStack> items = new ArrayList<>();
		if (tab == null || tab.equals(ItemTabs.COMPONENT_TAB)) {
			for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
				for (ItemComponentType item : new LinkedHashSet<>(def.getItemComponents())) {
					ItemStack stack = new ItemStack(this, 1);
					Data data = new Data(stack);
					data.def = def;
					data.componentType = item;
					data.write();
					applyCustomName(stack);
					items.add(stack);
				}
			}
		}
		return items;
	}

	@Override
	public List<String> getTooltip(ItemStack stack) {
		List<String> tooltip = new ArrayList<>();
		Data data = new Data(stack);
		tooltip.add(GuiText.GAUGE_TOOLTIP.toString(data.gauge));
		if (data.requiresHammering()) {
			tooltip.add(TextColor.RED.wrap(GuiText.RAW_CAST_TOOLTIP.toString()));
		}
		return tooltip;
	}

	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (new Data(player.getHeldItem(hand)).componentType != ItemComponentType.FRAME) {
			return ClickResult.REJECTED;
		}
		
		return tryPlaceStock(player, world, pos, hand, CollectionUtil.listOf(ItemComponentType.FRAME));
	}

	public static class Data extends BaseItemRollingStock.Data {
		@TagField("componentType")
		public ItemComponentType componentType;
		@TagField("raw_cast")
		public boolean rawCast;

		public Data(ItemStack stack) {
			super(stack);
			if (componentType == null) {
				componentType = ItemComponentType.FRAME;
			}
		}


		public boolean requiresHammering() {
			return componentType.crafting == CraftingType.CASTING_HAMMER && rawCast;
		}
	}
}
