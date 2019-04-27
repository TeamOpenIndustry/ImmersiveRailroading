package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

import java.util.List;

public class ItemTrackBlueprint extends ItemBase {
	public ItemTrackBlueprint() {
		super(ImmersiveRailroading.MODID, "item_rail", 1, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (world.isClient && hand == Hand.PRIMARY) {
            player.internal.openGui(ImmersiveRailroading.instance, GuiTypes.RAIL.ordinal(), world.internal, (int) player.internal.posX, (int) player.internal.posY, (int) player.internal.posZ);
        }
	}
	
	@Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);

		if (world.isServer && hand == Hand.SECONDARY) {
			RailSettings info = settings(stack);
			ItemStack blockinfo = new ItemStack(world.getBlock(pos).getItem(world.internal, pos.internal, world.internal.getBlockState(pos.internal)));
			if (player.isCrouching()) {
				info = new RailSettings(
                    info.gauge,
                    info.track,
                    info.type,
                    info.length,
                    info.quarters,
                    info.posType,
                    info.direction,
                    info.railBed,
                    blockinfo,
                    info.isPreview,
                    info.isGradeCrossing
                );
			} else {
				info = new RailSettings(
                    info.gauge,
                    info.track,
                    info.type,
                    info.length,
                    info.quarters,
                    info.posType,
                    info.direction,
                    blockinfo,
                    info.railBedFill,
                    info.isPreview,
                    info.isGradeCrossing
				);
			}
			settings(stack, info);
			return ClickResult.ACCEPTED;
		}

		pos = pos.up();
		
		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			if (!BlockUtil.isIRRail(world, pos.down()) || world.getTileEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
				pos = pos.down();
			}
		}
		PlacementInfo placementInfo = new PlacementInfo(stack, player.getYawHead(), pos, hit);
		
		if (settings(stack).isPreview) {
			if (!BlockUtil.canBeReplaced(world, pos, false)) {
				pos = pos.up();
			}
			world.setBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW);
			TileRailPreview te = world.getTileEntity(pos, TileRailPreview.class);
			if (te != null) {
				te.setup(stack, placementInfo);
			}
			return ClickResult.ACCEPTED;
		}

		RailInfo info = new RailInfo(world, stack, placementInfo, null);
		info.build(player);
		return ClickResult.ACCEPTED;
    }

	@Override
	public void addInformation(ItemStack stack, List<String> tooltip) {
        RailSettings settings = settings(stack);
        tooltip.add(GuiText.TRACK_TYPE.toString(settings.type));
        tooltip.add(GuiText.TRACK_GAUGE.toString(settings.gauge));
        tooltip.add(GuiText.TRACK_LENGTH.toString(settings.length));
        tooltip.add(GuiText.TRACK_POSITION.toString(settings.posType));
        tooltip.add(GuiText.TRACK_DIRECTION.toString(settings.direction));
        tooltip.add(GuiText.TRACK_RAIL_BED.toString(settings.railBed.getDisplayName()));
        tooltip.add(GuiText.TRACK_RAIL_BED_FILL.toString(settings.railBedFill.getDisplayName()));
        tooltip.add((settings.isPreview ? GuiText.TRACK_PLACE_BLUEPRINT_TRUE : GuiText.TRACK_PLACE_BLUEPRINT_FALSE).toString());
        tooltip.add(GuiText.TRACK_QUARTERS.toString(settings.quarters * 90.0/4 ));
	}

	public static void settings(ItemStack stack, RailSettings settings) {
		stack.setTagCompound(settings.toNBT());
	}
	
	public static RailSettings settings(ItemStack stack) {
		return new RailSettings(stack.getTagCompound());
	}
}
