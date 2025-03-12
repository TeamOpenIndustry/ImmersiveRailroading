package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
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

import java.util.Collections;
import java.util.List;

public class ItemGoldenSpike extends CustomItem {
	public ItemGoldenSpike() {
		super(ImmersiveRailroading.MODID, "item_golden_spike");

		Fuzzy gold = Fuzzy.GOLD_INGOT;
		Recipes.shapedRecipe(this, 2,
				gold, gold, gold, null, gold, null);
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
					//If x or z < 0 there are some weird things...
					Vec3d finalPos = hit.subtract(0, hit.y, 0).add(pos).subtract(tepos);
					if(finalPos.x < 0){
						finalPos = finalPos.add(1,0,0);
					}
					if(finalPos.z < 0){
						finalPos = finalPos.add(0,0,1);
					}
					tr.setCustomInfo(new PlacementInfo(tr.getItem(), player.getYawHead(), finalPos));
				}
			}
		}
		return ClickResult.ACCEPTED;
	}

	@Override
	public void onClickAir(Player player, World world, Player.Hand hand) {
		if (world.isClient) {
			Data d = new Data(player.getHeldItem(hand));
			if (d.pos != null) {
				TileRailPreview te = world.getBlockEntity(d.pos, TileRailPreview.class);
				if (te != null) {
					GuiTypes.RAIL_PREVIEW.open(player, d.pos);
				}
			}
		}
	}

	public static class Data extends ItemDataSerializer {
		@TagField("pos")
		public Vec3i pos;

		protected Data(ItemStack stack) {
			super(stack);
		}
	}
}
