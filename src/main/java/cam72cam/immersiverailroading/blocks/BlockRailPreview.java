package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.BlockSettings;
import cam72cam.mod.block.IBreakCancelable;
import cam72cam.mod.block.Material;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

public class BlockRailPreview extends BlockTypeEntity {
	public BlockRailPreview() {
		super(new BlockSettings(ImmersiveRailroading.MODID, "block_rail_preview")
				.withMaterial(Material.WOOL)
				.withHardness(0.2F)
				.withExplosionResistance(2000)
				.withConnectable(false)
		, TileRailPreview::new);
	}

	/*

	Custom BlockType for IBreakCancellable

	 */

	protected class RailBlockInternal extends BlockInternal implements IBreakCancelable {
		@Override
		public boolean tryBreak(World world, Vec3i pos, Player player) {
			return BlockRailPreview.this.tryBreak(world, pos, player);
		}
	}

	@Override
	protected BlockInternal getBlock() {
		return new RailBlockInternal();
	}

	public boolean tryBreak(World world, Vec3i pos, Player entityPlayer) {
		if (entityPlayer.isCrouching()) {
			TileRailPreview tr = world.getBlockEntity(pos, TileRailPreview.class);
			if (tr != null) {
				//internal.setBlockToAir(pos);
				tr.getRailRenderInfo().build(entityPlayer);
				return true;
			}
		}
		return false;
	}
}
