package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.TextUtil;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemRailAugment extends CustomItem {
	public ItemRailAugment() {
		super(ImmersiveRailroading.MODID, "item_augment");
	}

	@Override
	public int getStackSize() {
		return 16;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}


	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world, pos) && player.hasPermission(Permissions.AUGMENT_TRACK)) {
			TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
			if (te != null) {
				ItemStack stack = player.getHeldItem(hand);
				Data data = new Data(stack);
				if (te.getAugment() == null && (player.isCreative() || Gauge.from(te.getTrackGauge()) == data.gauge)) {
					TileRail parent = te.getParentTile();
					if (parent == null) {
						return ClickResult.REJECTED;
					}
					switch(data.augment) {
					case WATER_TROUGH:
						return ClickResult.REJECTED;
						/*
						if (parent.getRotationQuarter() != 0) {
							return EnumActionResult.FAIL;
						}
						if (parent.getType() != TrackItems.STRAIGHT) {
							return EnumActionResult.FAIL; 
						}
						break;
						*/
					case SPEED_RETARDER:
						switch(parent.info.settings.type) {
						case SWITCH:
						case TURN:
							return ClickResult.REJECTED;
						default:
							break;
						}
					default:
						break;
					}

					if(world.isServer) {
						te.setAugment(data.augment);
						if (!player.isCreative()) {
							stack.setCount(stack.getCount()-1);
						}
					}
					return ClickResult.ACCEPTED;
				}
			}
		}
		return ClickResult.PASS;
	}

	@Override
	public List<ItemStack> getItemVariants(CreativeTab tab)
    {
		List<ItemStack> items = new ArrayList<>();
        if (tab == null || tab.equals(ItemTabs.MAIN_TAB))
        {
        	for (Augment augment : Augment.values()) {
        		if (augment == Augment.WATER_TROUGH) {
        			continue;
        		}
        		ItemStack stack = new ItemStack(this, 1);
				Data data = new Data(stack);
				data.augment = augment;
				data.write();
                items.add(stack);
        	}
        }
		return items;
    }
	
	@Override
	public List<String> getTooltip(ItemStack stack)
    {
        return Collections.singletonList(GuiText.GAUGE_TOOLTIP.toString(new Data(stack).gauge));
    }

	@Override
	public String getCustomName(ItemStack stack) {
		return TextUtil.translate("item.immersiverailroading:item_augment." + new Data(stack).augment.name() + ".name");
	}

	public static class Data extends ItemDataSerializer {
		@TagField("gauge")
		public Gauge gauge;
		@TagField("augment")
		public Augment augment;

		public Data(ItemStack stack) {
			super(stack);
			if (gauge == null) {
				gauge = Gauge.from(Gauge.STANDARD);
			}
			if (augment == null) {
				augment = Augment.SPEED_RETARDER;
			}
		}
	}
}
