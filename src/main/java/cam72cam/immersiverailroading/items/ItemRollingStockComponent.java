package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemRawCast;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TextColor;

public class ItemRollingStockComponent extends BaseItemRollingStock {
	public static final String NAME = "item_rolling_stock_component";
	
	public ItemRollingStockComponent() {
		super(ImmersiveRailroading.MODID, "item_rolling_stock_component", 64, ItemTabs.COMPONENT_TAB);
	}
	
	@Override
	public String getCustomName(ItemStack stack) {
		return super.getCustomName(stack) + " " + ItemComponent.getComponentType(stack).toString();
	}

	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab) {
		List<ItemStack> items = new ArrayList<>();
		if (tab == null || tab.equals(ItemTabs.COMPONENT_TAB)) {
			for (String defID : DefinitionManager.getDefinitionNames()) {
				EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
				for (ItemComponentType item : new LinkedHashSet<>(def.getItemComponents())) {
					ItemStack stack = new ItemStack(this, 1);
					ItemDefinition.setID(stack, defID);
					ItemComponent.setComponentType(stack, item);
					ItemRawCast.set(stack, false);
					applyCustomName(stack);
					items.add(stack);
				}
			}
		}
		return items;
	}

	public static boolean requiresHammering(ItemStack stack) {
		return ItemComponent.getComponentType(stack).crafting == CraftingType.CASTING_HAMMER && ItemRawCast.get(stack); 
	}
	
	@Override
	public void addInformation(ItemStack stack, List<String> tooltip) {
		tooltip.add(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
		if (requiresHammering(stack)) {
			tooltip.add(TextColor.RED.wrap(GuiText.RAW_CAST_TOOLTIP.toString()));
		}
	}

	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (ItemComponent.getComponentType(player.getHeldItem(hand)) != ItemComponentType.FRAME) {
			return ClickResult.REJECTED;
		}
		
		List<ItemComponentType> frame = new ArrayList<ItemComponentType>();
		frame.add(ItemComponentType.FRAME);
		return tryPlaceStock(player, world, pos, hand, frame);
	}
}
