package cam72cam.immersiverailroading.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.BuilderBase;

public class TileRail extends TileRailBase {
	private EnumFacing facing;
	private TrackItems type;
	
	private BlockPos center;
	
	private boolean isVisible = true;
	private boolean switchActive = false;
	
	private int length;
	private int rotationQuarter;
	private TrackDirection direction = TrackDirection.NONE;
	private int turnQuarters;
	private float horizOff;


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
	public void setFacing(EnumFacing value) {
		this.facing = value;
		this.markDirty();
	}
	
	public TrackItems getType() {
		return this.type;
	}
	public void setType(TrackItems value) {
		this.type = value;
		this.markDirty();
	}
	
	public boolean getSwitchState() {
		return switchActive;
	}
	
	public boolean isVisible() {
		return isVisible;
	}
	public void setVisible(Boolean value) {
		this.isVisible = value;
		this.markDirty();
	}

	public Vec3i getCenter() {
		return center;
	}
	public double getRadius() {
		return length;
	}
	public void setCenter(BlockPos center) {
		this.center = center;
		this.markDirty();
	}

	public TrackDirection getDirection() {
		return this.direction;
	}
	public void setDirection(TrackDirection dir) {
		this.direction = dir;
		this.markDirty();
	}
	
	
	public float getHorizOff() {
		return horizOff;
	}
	public void setHorizOff(float horizOff) {
		this.horizOff = horizOff;
		this.markDirty();
	}
	
	
	/*
	 * Either blocks or quarters
	 */
	public int getLength() {
		return this.length;
	}
	public double getSlope() {
		return 1 / this.length;
	}
	public void setLength(int length) {
		this.length = length;
		this.markDirty();
	}

	public int getRotationQuarter() {
		return this.rotationQuarter;
	}
	public void setRotationQuarter(int val) {
		this.rotationQuarter = val;
		this.markDirty();
	}

	public int getTurnQuarters() {
		return this.turnQuarters;
	}
	public void setTurnQuarters(int quarters) {
		this.turnQuarters = quarters;
		this.markDirty();
	}
	

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		facing = EnumFacing.getFront(nbt.getByte("facing"));
		type = TrackItems.valueOf(nbt.getString("type"));
		
		center = getNBTBlockPos(nbt, "center");
		
		isVisible = nbt.getBoolean("isVisible");
		switchActive = nbt.getBoolean("switchActive");
		
		horizOff = nbt.getFloat("horizOff");
		
		length = nbt.getInteger("length");
		rotationQuarter = nbt.getInteger("rotationQuarter");
		direction = TrackDirection.values()[nbt.getInteger("direction")];
		turnQuarters = nbt.getInteger("turnQuarters");
		
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("facing", (byte) facing.getIndex());
		nbt.setString("type", type.name());
		
		setNBTBlockPos(nbt, "center", center);
		
		nbt.setBoolean("isVisible", isVisible);
		nbt.setBoolean("switchActive", switchActive);
		
		nbt.setFloat("horizOff", horizOff);
		
		nbt.setInteger("length", length);
		nbt.setInteger("rotationQuarter", rotationQuarter);
		nbt.setInteger("direction", direction.ordinal());
		nbt.setInteger("turnQuarters", turnQuarters);
		
		return super.writeToNBT(nbt);
	}

	private BuilderBase builder;
	public BuilderBase getBuilder() {
		if (builder == null) {
			builder = getType().getBuilder(this, new BlockPos(0,0,0));
		}
		return builder;
	}
}
