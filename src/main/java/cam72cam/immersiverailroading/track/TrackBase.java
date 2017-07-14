package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.track.BuilderBase.PosRot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public abstract class TrackBase {
	public BuilderBase builder;

	private int rel_x;
	private int rel_y;
	private int rel_z;
	private EnumFacing rel_rotation;

	protected Block block;

	public TrackBase(BuilderBase builder, int rel_x, int rel_y, int rel_z, Block block, EnumFacing rel_rotation) {
		this.builder = builder;
		this.rel_x = rel_x;
		this.rel_y = rel_y;
		this.rel_z = rel_z;
		this.rel_rotation = rel_rotation;
		this.block = block;
	}

	private boolean canBeReplaced() {
		PosRot pos = builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
		Block block = builder.world.getBlockState(pos).getBlock();
		
		return block == null || block.isReplaceable(builder.world, pos) || block instanceof BlockFlower || block == Blocks.DOUBLE_PLANT || block instanceof BlockMushroom;
	}

	@SuppressWarnings("deprecation")
	public boolean canPlaceTrack() {
		PosRot pos = builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
		
		return canBeReplaced() && builder.world.getBlockState(pos.down()).isTopSolid();
	}

	public TileEntity placeTrack() {
		PosRot pos = builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
		
		IBlockState state = builder.world.getBlockState(pos);
		Block removed = state.getBlock();
		if (removed != null) {
			removed.dropBlockAsItem(builder.world, pos, state, 0);
		}
		builder.world.setBlockState(pos, getBlockState(), 3);
		return builder.world.getTileEntity(pos);
	}
	public IBlockState getBlockState() {
		return block.getDefaultState();
	}
	public EnumFacing getFacing() {
		PosRot coords = builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
		return coords.getRotation();
	}

	public void moveTo(TrackBase trackBase) {
		rel_x = trackBase.rel_x;
		rel_y = trackBase.rel_y;
		rel_z = trackBase.rel_z;
	}

}
