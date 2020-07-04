package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.mod.item.*;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.sound.StandardSound;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;

public class ItemGoldenSpike extends ItemBase {
	public ItemGoldenSpike() {
		super(ImmersiveRailroading.MODID, "item_golden_spike", 1, ItemTabs.MAIN_TAB);

		Fuzzy gold = Fuzzy.GOLD_INGOT;
		Recipes.register(this, 2,
				gold, gold, gold, null, gold, null);
	}

	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		ItemStack held = player.getHeldItem(hand);
		if (world.isBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW)) {
			Data d = new Data(held);
			d.pos = pos;
			d.write();
			Audio.playSound(world, pos, StandardSound.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 0.2f);
		} else {
			pos = pos.up();

			Vec3i tepos = new Data(held).pos;
			if (tepos != null) {
				if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
					if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
						pos = pos.down();
					}
				}
				TileRailPreview tr = world.getBlockEntity(tepos, TileRailPreview.class);
				if (tr != null) {
					if (tr.isAboveRails()) {
						tepos = tepos.down();
					}
					tr.setCustomInfo(new PlacementInfo(tr.getItem(), player.getYawHead(), hit.subtract(0, hit.y, 0).add(pos).subtract(tepos)));
				}
			}
		}
		return ClickResult.PASS;
	}

	public static class Data extends ItemData {
		@TagField("pos")
		public Vec3i pos;

		protected Data(ItemStack stack) {
			super(stack);
		}
	}
}
