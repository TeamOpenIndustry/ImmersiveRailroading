package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.mod.ItemStack;
import cam72cam.mod.Player;
import cam72cam.mod.World;
import cam72cam.mod.block.BlockEntityBase;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.IBreakCancelable;
import cam72cam.mod.block.Material;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class BlockRailPreview extends BlockEntityBase<TileRailPreview> implements IBreakCancelable {
	public BlockRailPreview() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail_preview")
				.withMaterial(Material.WOOL)
				.withHardness(0.2F)
				.withExplosionResistance(2000)
				.withConnectable(false)
				.withBlockEntity(TileRailPreview::new)
		);
	}

	@Override
	public void onBreak(TileRailPreview entity) {

	}

	@Override
	public boolean onClick(TileRailPreview te, Player player, Hand hand, Facing facing, Vec3d hit) {
		if (player.isCrouching()) {
			Vec3i pos = te.pos;
			World world = te.world;
			if (world.isServer) {
                if (BlockUtil.canBeReplaced(world.internal, pos.down().internal, true)) {
                    if (!BlockUtil.isIRRail(world.internal, pos.down().internal) || world.getTileEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
                        pos = pos.down();
                    }
                }
                te.setPlacementInfo(new PlacementInfo(te.getItem(), player.internal.rotationYawHead, pos, hit));
			}
			return false;
		} else {
			if (player.getHeldItem(hand).item == IRItems.ITEM_GOLDEN_SPIKE) {
				return false;
			}
			//TODO player.openGui(ImmersiveRailroading.instance, GuiTypes.RAIL_PREVIEW.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

	@Override
	public ItemStack onPick(TileRailPreview entity) {
		return new ItemStack(entity.getItem());
	}

	@Override
	public void onNeighborChange(TileRailPreview entity, Vec3i neighbor) {

	}



	public boolean tryBreak(World world, Vec3i pos, Player entityPlayer) {
		if (entityPlayer.isCrouching()) {
			TileRailPreview tr = TileRailPreview.get(world.internal, pos.internal);
			if (tr != null) {
				//internal.setBlockToAir(pos);
				tr.getRailRenderInfo().build(entityPlayer.internal);
				return true;
			}
		}
		return false;
	}

	@Override
	public double getHeight(TileRailPreview te) {
		return 0.125;
	}
}
