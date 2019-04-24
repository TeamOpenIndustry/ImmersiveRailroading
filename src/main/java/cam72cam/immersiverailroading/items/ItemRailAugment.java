package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemAugmentType;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class ItemRailAugment extends ItemBase {
	public ItemRailAugment() {
		super(ImmersiveRailroading.MODID, "item_augment", 16, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world.internal, pos.internal)) {
			TileRailBase te = world.getTileEntity(pos, TileRailBase.class);
			if (te != null) {
				ItemStack stack = player.getHeldItem(hand);
				if (te.getAugment() == null && (player.isCreative() || Gauge.from(te.getTrackGauge()) == ItemGauge.get(stack))) {
					Augment augment = ItemAugmentType.get(stack);
					TileRail parent = te.getParentTile();
					if (parent == null) {
						return ClickResult.REJECTED;
					}
					switch(augment) {
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
						te.setAugment(augment);
						if (!player.isCreative()) {
							stack.setCount(stack.getCount()-1);;
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
        		ItemAugmentType.set(stack, augment);
                items.add(stack);
        	}
        }
		return items;
    }
	
	@Override
	public void addInformation(ItemStack stack, List<String> tooltip)
    {
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
    }

    /* TODO
	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		return super.getUnlocalizedName() + "." + ItemAugmentType.get(stack).name();
	}
	*/
}
