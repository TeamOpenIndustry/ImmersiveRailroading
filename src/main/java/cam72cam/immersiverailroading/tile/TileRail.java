package cam72cam.immersiverailroading.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import cam72cam.immersiverailroading.library.TrackType;

public class TileRail extends TileRailBase {
	private EnumFacing facing;
	private TrackType type;
	
	private BlockPos center;

	private double curveRadius;
	
	private double slopeHeight;
	private double slopeLength;
	private double slopeAngle;
	
	private boolean isVisible = true;
	private boolean switchActive = false;


	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return Double.MAX_VALUE;
	}

	public EnumFacing getFacing() {
		if (facing == EnumFacing.DOWN) {
			return EnumFacing.NORTH;
		}
		return facing;
	}
	
	public TrackType getType() {
		return this.type;
	}
	
	public boolean getSwitchState() {
		return switchActive;
	}
	public boolean isVisible() {
		return isVisible;
	}

	public Vec3i getCenter() {
		return center;
	}

	public double getRadius() {
		return curveRadius;
	}
	
	public void setType(TrackType value) {
		this.type = value;
		this.markDirty();
	}
	public void setFacing(EnumFacing value) {
		this.facing = value;
		this.markDirty();
	}
	public void setVisible(Boolean value) {
		this.isVisible = value;
		this.markDirty();
	}
	public void setCenter(BlockPos center, float radius) {
		this.center = center;
		this.curveRadius = radius;
		this.markDirty();
	}
	public void setSlope(float slopeAngle, int slopeHeight, int slopeLength) {
		this.slopeAngle = slopeAngle;
		this.slopeHeight = slopeHeight;
		this.slopeLength = slopeLength;
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		facing = EnumFacing.getFront(nbt.getByte("facing"));
		type = TrackType.valueOf(nbt.getString("type"));
		
		center = getNBTBlockPos(nbt, "center");
		curveRadius = nbt.getDouble("r");
		
		slopeHeight = nbt.getDouble("slopeHeight");
		slopeLength = nbt.getDouble("slopeLength");
		slopeAngle = nbt.getDouble("slopeAngle");
		
		isVisible = nbt.getBoolean("isVisible");
		switchActive = nbt.getBoolean("switchActive");
		
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("facing", (byte) facing.getIndex());
		nbt.setString("type", type.name());
		
		setNBTBlockPos(nbt, "center", center);
		nbt.setDouble("r", curveRadius);
		
		nbt.setDouble("slopeHeight", slopeHeight);
		nbt.setDouble("slopeLength", slopeLength);
		nbt.setDouble("slopeAngle", slopeAngle);
		
		nbt.setBoolean("isVisible", isVisible);
		nbt.setBoolean("switchActive", switchActive);
		
		return super.writeToNBT(nbt);
	}

	/*
	@Override
	public void update() {
		if (world.isRemote) {
			return;
		}

		if (this.areEntitiesOnSwitch()) {
			// Force switch certain direction
			setSwitchState(true);
		} else {
			// Default to redstone
			setSwitchState(world.isBlockIndirectlyGettingPowered(this.pos) > 0);
		}
	}*/
	
	/*
	 * 
    
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    	TileRail tr = (TileRail) world.getTileEntity(pos);
    	if (tr != null) {
    		System.out.println(String.format("GOT FACING %s %s", tr.getFacing(), ((World)world).isRemote));
    		return state.withProperty(BlockRail.FACING, tr.getFacing()).withProperty(BlockRail.TRACK_TYPE, tr.getType());
    	}
    	System.out.println("TILE NOT FOUND");
    	return this.getDefaultState();
    }

	public void setSwitchState(boolean state) {
		if (!this.isSwitch()) {
			return;
		}

		if (switchActive == state) {
			return;
		}

		this.switchActive = state;

		this.markDirty();
		this.markBlockForUpdate();
		
		int i = this.pos.getX();
		int j = this.pos.getY();
		int k = this.pos.getZ();

		TileEntity te1 = null;
		TileEntity te2 = null;

		switch (facingMeta) {
		case 0:
			te1 = world.getTileEntity(new BlockPos(i, j, k + 1));
			te2 = world.getTileEntity(new BlockPos(i, j, k + 2));
			break;
		case 1:
			te1 = world.getTileEntity(new BlockPos(i - 1, j, k));
			te2 = world.getTileEntity(new BlockPos(i - 2, j, k));
			break;
		case 2:
			te1 = world.getTileEntity(new BlockPos(i, j, k - 1));
			te2 = world.getTileEntity(new BlockPos(i, j, k - 2));
			break;
		case 3:
			te1 = world.getTileEntity(new BlockPos(i + 1, j, k));
			te2 = world.getTileEntity(new BlockPos(i + 2, j, k));
		}

		if (!this.switchActive) {
			((TileRail) te1).setType(TrackType.STRAIGHT_SMALL);
			if (type == TrackType.MEDIUM_RIGHT_PARALLEL_SWITCH || type == TrackType.MEDIUM_LEFT_PARALLEL_SWITCH
					|| type == TrackType.LARGE_RIGHT_SWITCH || type == TrackType.LARGE_LEFT_SWITCH) {
				((TileRail) te2).setType(TrackType.STRAIGHT_SMALL);
			}
		} else {
			switch (type) {
			case MEDIUM_LEFT_PARALLEL_SWITCH:
				((TileRail) te2).setType(TrackType.MEDIUM_LEFT_TURN);
			case MEDIUM_LEFT_SWITCH:
				((TileRail) te1).setType(TrackType.MEDIUM_LEFT_TURN);
				break;

			case MEDIUM_RIGHT_PARALLEL_SWITCH:
				((TileRail) te2).setType(TrackType.MEDIUM_RIGHT_TURN);
			case MEDIUM_RIGHT_SWITCH:
				((TileRail) te1).setType(TrackType.MEDIUM_RIGHT_TURN);
				break;

			case LARGE_RIGHT_SWITCH:
				((TileRail) te2).setType(TrackType.TURN_LARGE_RIGHT);
				((TileRail) te1).setType(TrackType.TURN_LARGE_RIGHT);
				break;
			case LARGE_LEFT_SWITCH:
				((TileRail) te2).setType(TrackType.TURN_LARGE_LEFT);
				((TileRail) te1).setType(TrackType.TURN_LARGE_LEFT);
				break;
			default:
				break;
			}
		}
	}

	public boolean isTurnTrack() {
		return type.getType() == TrackItems.TURN || type.getType() == TrackItems.SWITCH && getSwitchState();
	}

	public boolean isStraightTrack() {
		return type.getType() == TrackItems.STRAIGHT || type.getType() == TrackItems.CROSSING || type.getType() == TrackItems.SWITCH
				&& !getSwitchState();
	}

	public boolean isTwoWaysCrossingTrack() {
		return type == TrackType.TWO_WAYS_CROSSING;
	}

	public boolean isSwitch() {
		return type.getType() == TrackItems.SWITCH;
	}

	public boolean isRightSwitch() {
		return TrackType.MEDIUM_RIGHT_SWITCH == type || TrackType.LARGE_RIGHT_SWITCH == type || TrackType.MEDIUM_RIGHT_PARALLEL_SWITCH == type;
	}

	public boolean isLeftSwitch() {
		return TrackType.MEDIUM_LEFT_SWITCH == type || TrackType.LARGE_LEFT_SWITCH == type || TrackType.MEDIUM_LEFT_PARALLEL_SWITCH == type;
	}

	public boolean isSlopeTrack() {
		return type.getType() == TrackItems.SLOPE;
	}

	public boolean areEntitiesOnSwitch() {
		float[] offsets = new float[] { 0, 0, 0, 0, 0, 0 };
		switch (facingMeta) {
		case 0:
			if (isLeftSwitch()) {
				offsets = new float[] { -2.0F, 0, 2.0F, f, 1.0F - f, 2.0F - f };
			} else {
				offsets = new float[] { +1.0F, 0, 1.0F, 2.0F - f, 1.0F - f, 2.0F - f };
			}
			break;

		case 1:
			if (isLeftSwitch()) {
				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, f };
			} else {

				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, 2.0F - f };
			}
			break;

		case 2:
			if (isLeftSwitch()) {
				offsets = new float[] { +1.0F, 0, 1.0F, 2.0F - f, 1.0F - f, f };
			} else {

				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, f };
			}
			break;

		case 3:
			if (isLeftSwitch()) {
				offsets = new float[] { +1.0F, 0, 1.0F, 3.0F - f, 1.0F - f, 2.0F - f };
			} else {
				offsets = new float[] { +1.0F, 0, 1.0F, 3.0F - f, 1.0F + f, f };
			}
			break;
		}
		return !world.getEntitiesWithinAABB(
				EntityMinecart.class,
				new AxisAlignedBB(this.pos.getX() + offsets[0], this.pos.getY() + offsets[1], this.pos.getZ() + offsets[2], this.pos.getX() + offsets[3],
						this.pos.getY() + offsets[4], this.pos.getZ() + offsets[5])).isEmpty();
	}*/
}
