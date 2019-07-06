package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

public class ItemGoldenSpike extends ItemBase {
	public ItemGoldenSpike() {
		super(ImmersiveRailroading.MODID, "item_golden_spike", 1, ItemTabs.MAIN_TAB);
	}

	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		ItemStack held = player.getHeldItem(hand);
		if (world.isBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW)) {
			setPosition(held, pos);
			world.internal.playSound(pos.x, pos.y, pos.z, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 0.2f, false);
		} else {
			pos = pos.up();
			
			Vec3i tepos = getPosition(held);
			if (tepos != null) {
				if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
					if (!BlockUtil.isIRRail(world, pos.down()) || world.getTileEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
						pos = pos.down();
					}
				}
				TileRailPreview tr = world.getTileEntity(tepos, TileRailPreview.class);
				if (tr != null) {
					tr.setCustomInfo(new PlacementInfo(tr.getItem(), player.getYawHead(), pos, hit));
				}
			}
		}
		return ClickResult.PASS;
	}

	public static Vec3i getPosition(ItemStack stack) {
		if (stack.getTagCompound().hasKey("pos")) {
			return stack.getTagCompound().getVec3i("pos");
		} else {
			return null;
		}
	}
	
	public static void setPosition(ItemStack stack, Vec3i pos) {
		stack.getTagCompound().setVec3i("pos", pos);
	}
}
