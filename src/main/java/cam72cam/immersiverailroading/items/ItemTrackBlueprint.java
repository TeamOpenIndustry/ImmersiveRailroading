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
import cam72cam.mod.item.*;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

import java.util.List;

public class ItemTrackBlueprint extends ItemBase {
	public ItemTrackBlueprint() {
		super(ImmersiveRailroading.MODID, "item_rail", 1, ItemTabs.MAIN_TAB);

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 3,
				steel, null, steel, steel, Fuzzy.PAPER, steel, steel, null, steel);
	}
	
	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (world.isClient && hand == Hand.PRIMARY) {
			GuiTypes.RAIL.open(player);
        }
	}
	
	@Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);

		if (world.isServer && hand == Hand.SECONDARY) {
			RailSettings info = settings(stack);
			ItemStack blockinfo = world.getItemStack(pos);
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
			if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
				pos = pos.down();
			}
		}
		PlacementInfo placementInfo = new PlacementInfo(stack, player.getYawHead(), pos, hit);
		
		if (settings(stack).isPreview) {
			if (!BlockUtil.canBeReplaced(world, pos, false)) {
				pos = pos.up();
			}
			world.setBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW);
			TileRailPreview te = world.getBlockEntity(pos, TileRailPreview.class);
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
	public List<String> getTooltip(ItemStack stack) {
        RailSettings settings = settings(stack);
        return CollectionUtil.listOf(
            GuiText.TRACK_TYPE.toString(settings.type),
            GuiText.TRACK_GAUGE.toString(settings.gauge),
            GuiText.TRACK_LENGTH.toString(settings.length),
            GuiText.TRACK_POSITION.toString(settings.posType),
            GuiText.TRACK_DIRECTION.toString(settings.direction),
            GuiText.TRACK_RAIL_BED.toString(settings.railBed.getDisplayName()),
            GuiText.TRACK_RAIL_BED_FILL.toString(settings.railBedFill.getDisplayName()),
            (settings.isPreview ? GuiText.TRACK_PLACE_BLUEPRINT_TRUE : GuiText.TRACK_PLACE_BLUEPRINT_FALSE).toString(),
            GuiText.TRACK_QUARTERS.toString(settings.quarters * 90.0/4 )
		);
	}

	public static void settings(ItemStack stack, RailSettings settings) {
		stack.setTagCompound(settings.toNBT());
	}
	
	public static RailSettings settings(ItemStack stack) {
		return new RailSettings(stack.getTagCompound());
	}
}
