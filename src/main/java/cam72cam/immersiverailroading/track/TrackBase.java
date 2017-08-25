package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.BuilderBase.PosRot;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

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

	@SuppressWarnings("deprecation")
	public boolean canPlaceTrack() {
		PosRot pos = getPos();
		
		return BlockUtil.canBeReplaced(builder.world, pos, true) && builder.world.getBlockState(pos.down()).isTopSolid();
	}

	public TileEntity placeTrack() {
		PosRot pos = getPos();
		
		NBTTagCompound replaced = null;
		
		IBlockState state = builder.world.getBlockState(pos);
		Block removed = state.getBlock();
		if (removed != null) {
			if (removed instanceof BlockRailBase) {
				TileRailBase te = (TileRailBase) builder.world.getTileEntity(pos);
				replaced = te.serializeNBT();
				te.setWillBeReplaced();
			} else {				
				removed.dropBlockAsItem(builder.world, pos, state, 0);
			}
		}
		builder.world.setBlockState(pos, getBlockState(), 3);
		TileRailBase tr = (TileRailBase)builder.world.getTileEntity(pos);
		tr.setReplaced(replaced);
		tr.setParent(builder.getPos());
		tr.setHeight(getHeight());
		return tr;
	}
	public IBlockState getBlockState() {
		return block.getDefaultState();
	}
	public EnumFacing getFacing() {
		return getPos().getRotation();
	}

	public void moveTo(TrackBase trackBase) {
		rel_x = trackBase.rel_x;
		rel_y = trackBase.rel_y;
		rel_z = trackBase.rel_z;
	}

	
	public PosRot getPos() {
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
