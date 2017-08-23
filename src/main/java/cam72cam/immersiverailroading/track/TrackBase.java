package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.BuilderBase.PosRot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public abstract class TrackBase {
	public BuilderBase builder;

	private int rel_x;
	private int rel_y;
	private int rel_z;
	private EnumFacing rel_rotation;
	private float height;

	protected Block block;

	private boolean flexible = false;

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
		
		if (block == null) {
			return true;
		}
		if (block.isReplaceable(builder.world, pos)) {
			return true;
		}
		if (block instanceof BlockFlower || block == Blocks.DOUBLE_PLANT || block instanceof BlockMushroom) {
			return true;
		}
		if (block instanceof BlockRailBase) {
			TileRailBase te = (TileRailBase) builder.world.getTileEntity(pos);
			return te.isFlexible();
		}
		return false;
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

	
	public BlockPos getPos() {
		return builder.convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
	}
	
	public void setHeight(float height) {
		this.height = height;
	}
	public float getHeight() {
		return height;
	}

	public void setFlexible() {
		this.flexible  = true;
	}

	public boolean isFlexible() {
		return this.flexible;
	}
}
