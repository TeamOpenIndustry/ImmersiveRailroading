package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemLargeWrench extends CustomItem {
    public static final ExecutorService pool = Executors.newFixedThreadPool(15);

	public ItemLargeWrench() {
		super(ImmersiveRailroading.MODID, "item_large_wrench");

		Fuzzy steel = Fuzzy.STEEL_INGOT;
		IRFuzzy.registerSteelRecipe(this, 3,
				null, steel, null,
				steel, steel, steel,
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
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world, pos)) {
			TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
			if (te != null) {
				Augment augment = te.getAugment();
				if (augment != null) {
					te.setAugment(null);

					if(world.isServer && player.hasPermission(Permissions.AUGMENT_TRACK)) {
						ItemStack stack = new ItemStack(IRItems.ITEM_AUGMENT, 1);
						ItemRailAugment.Data data = new ItemRailAugment.Data(stack);
						data.augment = augment;
						data.gauge = Gauge.from(te.getTrackGauge());
						data.write();
						world.dropItem(stack, pos);
					}
					return ClickResult.ACCEPTED;
				}
				if (world.isServer) {
					while (te != null) {
						System.out.println(te);
						TileRail parent = te.getParentTile();
						if (parent != null && parent.info.settings.type == TrackItems.TURNTABLE) {
							// Odd shaped turntables don't work
							parent.setTablePosition(VecUtil.toWrongYaw(new Vec3d(parent.getPos()).add(0.5, 0, 0.5).subtract(hit.add(pos))) + parent.info.placementInfo.yaw);
							break;
						}
						te = te.getReplacedTile();
					}
				}
			}
		} else if (player.hasPermission(Permissions.MACHINIST) && world.isServer) {
			for (String key : MultiblockRegistry.keys()) {
				if (MultiblockRegistry.get(key).tryCreate(world, player, pos)) {
					return ClickResult.ACCEPTED;
				}
			}
		}
		return ClickResult.PASS;
	}
}
