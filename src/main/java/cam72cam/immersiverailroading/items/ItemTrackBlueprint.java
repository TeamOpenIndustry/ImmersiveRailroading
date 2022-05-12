package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemTrackBlueprint extends CustomItem {
	public ItemTrackBlueprint() {
		super(ImmersiveRailroading.MODID, "item_rail");

		Fuzzy steel = Fuzzy.STEEL_INGOT;
		IRFuzzy.registerSteelRecipe(this, 3,
				steel, null, steel,
				steel, Fuzzy.PAPER, steel,
				steel, null, steel);
	}

	@Override
	public int getStackSize() {
		return 1;
	}

	@Override
	public List<CreativeTab> getCreativeTabs() {
		return Collections.singletonList(ItemTabs.MAIN_TAB);
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (world.isClient && hand == Player.Hand.PRIMARY) {
			GuiTypes.RAIL.open(player);
        }
	}
	
	@Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
		ItemStack stack = player.getHeldItem(hand);
		RailSettings stackInfo = RailSettings.from(stack);

		if (world.isServer && hand == Player.Hand.SECONDARY) {
			ItemStack blockinfo = world.getItemStack(pos);
			if (player.isCrouching()) {
				stackInfo = stackInfo.withBedFill(blockinfo);
			} else {
				stackInfo = stackInfo.withBed(blockinfo);
			}
			stackInfo.write(stack);
			return ClickResult.ACCEPTED;
		}

		pos = pos.up();
		
		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
				pos = pos.down();
			}
		}

		if (stackInfo.isPreview) {
			if (!BlockUtil.canBeReplaced(world, pos, false)) {
				pos = pos.up();
			}
			world.setBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW);
			TileRailPreview te = world.getBlockEntity(pos, TileRailPreview.class);
			if (te != null) {
				PlacementInfo placementInfo = new PlacementInfo(stack, player.getYawHead(), hit.subtract(0, hit.y, 0));
				te.setup(stack, placementInfo);
			}
			return ClickResult.ACCEPTED;
		}

		PlacementInfo placementInfo = new PlacementInfo(stack, player.getYawHead(), hit.subtract(0, hit.y, 0));
		RailInfo info = new RailInfo(stack, placementInfo, null);
		info.build(player, pos);
		return ClickResult.ACCEPTED;
    }

	@Override
	public List<String> getTooltip(ItemStack stack) {
        RailSettings settings = RailSettings.from(stack);
        return Arrays.asList(
            GuiText.TRACK_TYPE.toString(settings.type),
            GuiText.TRACK_GAUGE.toString(settings.gauge),
            GuiText.TRACK_LENGTH.toString(settings.length),
            GuiText.TRACK_POSITION.toString(settings.posType),
            GuiText.TRACK_SMOOTHING.toString(settings.smoothing),
            GuiText.TRACK_DIRECTION.toString(settings.direction),
            GuiText.TRACK_RAIL_BED.toString(settings.railBed.getDisplayName()),
            GuiText.TRACK_RAIL_BED_FILL.toString(settings.railBedFill.getDisplayName()),
            (settings.isPreview ? GuiText.TRACK_PLACE_BLUEPRINT_TRUE : GuiText.TRACK_PLACE_BLUEPRINT_FALSE).toString(),
            GuiText.TRACK_QUARTERS.toString(settings.degrees),
			GuiText.TRACK_CURVOSITY.toString(String.format("%.2f", settings.curvosity))
		);
	}

}
