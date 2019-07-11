package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.CarPassengerDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;
import cam72cam.mod.item.ArmorSlot;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class ItemRollingStock extends BaseItemRollingStock {

	public ItemRollingStock() {
		super(ImmersiveRailroading.MODID, "item_rolling_stock", 1, ItemTabs.STOCK_TAB, ItemTabs.LOCOMOTIVE_TAB, ItemTabs.PASSENGER_TAB);
	}
	
	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab)
    {
		List<ItemStack> items = new ArrayList<>();
    	for (String defID : DefinitionManager.getDefinitionNames()) {
    		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
    		if (tab != null) {
	    		if (def instanceof CarPassengerDefinition) {
	    			if (!tab.equals(ItemTabs.PASSENGER_TAB)) {
	    				continue;
	    			}
	    		} else if (def instanceof LocomotiveDefinition) {
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
    		ItemDefinition.setID(stack, defID);
    		applyCustomName(stack);
            /*if (def.textureNames.size() > 1) {
            	for (String texture : def.textureNames.keySet()) {
	            	ItemStack textured = stack.copy();
	            	ItemTextureVariant.set(textured, texture);
	            	items.add(textured);
            	}
            } else {
                items.add(stack);
            }*/
			items.add(stack);
    	}
    	return items;
    }

    @Override
    public void addInformation(ItemStack stack, List<String> tooltip)
    {
		Gauge gauge = ItemGauge.get(stack);
        EntityRollingStockDefinition def = ItemDefinition.get(stack);
        if (def != null) {
        	tooltip.addAll(def.getTooltip(gauge));
        }
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(gauge));
        String texture = ItemTextureVariant.get(stack);
        if (texture != null && def != null && def.textureNames.get(texture) != null) {
	        tooltip.add(GuiText.TEXTURE_TOOLTIP.toString(def.textureNames.get(texture)));
        }
    }
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world, pos)) {
			RailBase te = world.getBlockEntity(pos, RailBase.class);
			if (te.getAugment() != null) {
				switch(te.getAugment()) {
				case DETECTOR:
				case LOCO_CONTROL:
				case FLUID_LOADER:
				case FLUID_UNLOADER:
				case ITEM_LOADER:
				case ITEM_UNLOADER:
					if (world.isServer) {
						boolean set = te.setAugmentFilter(ItemDefinition.getID(player.getHeldItem(hand)));
						if (set) {
							player.sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(ItemDefinition.get(player.getHeldItem(hand)).name()));
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
}
