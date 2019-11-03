package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemAugmentType;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.item.*;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class ItemLargeWrench extends ItemBase {
	public ItemLargeWrench() {
		super(ImmersiveRailroading.MODID, "item_large_wrench", 1, ItemTabs.MAIN_TAB);

		Fuzzy steel = Fuzzy.STEEL_INGOT.example() != null ? Fuzzy.STEEL_INGOT : Fuzzy.IRON_INGOT;
		Recipes.register(this, 3,
				null, steel, null, steel, steel, steel, steel, null, steel);
	}

	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (BlockUtil.isIRRail(world, pos)) {
			TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
			if (te != null) {
				Augment augment = te.getAugment();
				if (augment != null) {
					te.setAugment(null);

					if(world.isServer) {
						ItemStack stack = new ItemStack(IRItems.ITEM_AUGMENT, 1);
						ItemAugmentType.set(stack, augment);
						ItemGauge.set(stack, Gauge.from(te.getTrackGauge()));
						world.dropItem(stack, pos);
					}
					return ClickResult.ACCEPTED;
				}
				TileRail parent = te.getParentTile();
				if (world.isServer) {
					if (parent != null && parent.info.settings.type == TrackItems.TURNTABLE) {
						parent.nextTablePos(player.isCrouching());
					}
				}
			}
		} else {
			for (String key : MultiblockRegistry.keys()) {
				if (MultiblockRegistry.get(key).tryCreate(world, pos)) {
					return ClickResult.ACCEPTED;
				}
			}
		}
		return ClickResult.PASS;
	}
}
