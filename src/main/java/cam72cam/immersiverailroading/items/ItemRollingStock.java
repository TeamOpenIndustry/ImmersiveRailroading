package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.registry.CarPassengerDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.ItemCastingCost;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ArmorSlot;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemRollingStock extends BaseItemRollingStock {

	public ItemRollingStock() {
		super(ImmersiveRailroading.MODID, "item_rolling_stock");
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Arrays.asList(ItemTabs.STOCK_TAB, ItemTabs.LOCOMOTIVE_TAB, ItemTabs.PASSENGER_TAB);
	}

	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab)
    {
		List<ItemStack> items = new ArrayList<>();
    	for (EntityRollingStockDefinition def : DefinitionManager.getDefinitions()) {
    		if (tab != null) {
				boolean isCabCar = (def instanceof LocomotiveDefinition && ((LocomotiveDefinition) def).isCabCar());
	    		if (def instanceof CarPassengerDefinition || isCabCar) {
	    			if (!tab.equals(ItemTabs.PASSENGER_TAB)) {
	    				continue;
	    			}
	    		} else if (def instanceof LocomotiveDefinition && !isCabCar) {
	    			if (!tab.equals(ItemTabs.LOCOMOTIVE_TAB)) {
	    				continue;
	    			}
	    		} else {
	    			if (!tab.equals(ItemTabs.STOCK_TAB)) {
	    				continue;
	    			}
	    		}
    		}
    		ItemStack stack = new ItemStack(this, 1);
    		Data data = new Data(stack);
    		data.def = def;
    		data.gauge = def.recommended_gauge;
    		data.write();
            if (tab == null && def.textureNames.size() > 1 && ConfigGraphics.stockItemVariants) {
            	for (String texture : def.textureNames.keySet()) {
	            	ItemStack textured = stack.copy();
	            	data = new Data(textured);
	            	data.texture = texture;
	            	data.write();
	            	items.add(textured);
            	}
            } else {
                items.add(stack);
            }
    	}
    	return items;
    }

	public List<ItemStack> getCastableComponents(ItemStack stock) {
		if (!stock.is(IRItems.ITEM_ROLLING_STOCK)) {
			return Collections.emptyList();
		}
		EntityRollingStockDefinition def = new ItemRollingStockComponent.Data(stock).def;
		Gauge gauge = new ItemRollingStockComponent.Data(stock).gauge;
		return def.getItemComponents().stream()
				.filter(c -> !c.isWooden(def) &&
						(c.crafting == CraftingType.CASTING || c.crafting == CraftingType.CASTING_HAMMER) &&
						c.getCastCost(def, gauge) != ItemCastingCost.BAD_CAST_COST
				).map(c -> {
					ItemStack item = new ItemStack(IRItems.ITEM_ROLLING_STOCK_COMPONENT, 1);
					ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(item);
					data.def = def;
					data.gauge = gauge;
					data.componentType = c;
					data.rawCast = true;
					data.write();
					return item;
				})
				.collect(Collectors.toList());
	}

	@Override
    public List<String> getTooltip(ItemStack stack)
    {
    	List<String> tooltip = new ArrayList<>();

    	Data data = new Data(stack);

		Gauge gauge = data.gauge;
        EntityRollingStockDefinition def = data.def;
        if (def != null) {
        	tooltip.addAll(def.getTooltip(gauge));
        }
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(gauge));
        String texture = data.texture;
        if (texture != null && def != null && def.textureNames.get(texture) != null) {
	        tooltip.add(GuiText.TEXTURE_TOOLTIP.toString(def.textureNames.get(texture)));
        }

        tooltip.addAll(def.getExtraTooltipInfo());

        return tooltip;
    }
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world, pos)) {
			TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
			if (te.getAugment() != null) {
				switch(te.getAugment()) {
				case DETECTOR:
				case LOCO_CONTROL:
				case FLUID_LOADER:
				case FLUID_UNLOADER:
				case ITEM_LOADER:
				case ITEM_UNLOADER:
					if (world.isServer) {
						Data data = new Data(player.getHeldItem(hand));
						boolean set = te.setAugmentFilter(data.def != null ? data.def.defID : null);
						if (set) {
							player.sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(data.def != null ? data.def.name() : "Unknown"));
						} else {
							player.sendMessage(ChatText.RESET_AUGMENT_FILTER.getMessage());
						}
					}
					return ClickResult.ACCEPTED;
				default:
					break;
				}
			}
		}
		return tryPlaceStock(player, world, pos, hand, null);
	}
	
	@Override
	public boolean isValidArmor(ItemStack stack, ArmorSlot armorType, Entity entity) {
		return armorType == ArmorSlot.HEAD && ConfigGraphics.trainsOnTheBrain;
	}

	public static class Data extends BaseItemRollingStock.Data {
		public Data(ItemStack stack) {
			super(stack);
		}
	}
}
