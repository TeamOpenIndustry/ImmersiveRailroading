package cam72cam.immersiverailroading.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;

public class TileRail extends TileRailBase {
	private EnumFacing facing;
	private TrackItems type;
	private ItemStack railBed;
	
	private Vec3d center;
	
	private SwitchState switchState = SwitchState.NONE;
	
	private int length;
	private int rotationQuarter;
	private TrackDirection direction = TrackDirection.NONE;
	private int turnQuarters;
	
	private Vec3d placementPosition;
	
	private List<ItemStack> drops;
	
	public boolean snowRenderFlagDirty = true;


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
	public ItemStack getRailBed() {
		return this.railBed;
	}
	public void setRailBed(ItemStack railBed) {
		this.railBed = railBed;
		this.markDirty();
	}
	
	public SwitchState getSwitchState() {
		return switchState;
	}
	public void setSwitchState(SwitchState state) {
		if (state != switchState) {
			this.switchState = state;
			this.markDirty();
		}
	}

	public Vec3d getCenter() {
		if (center == null) {
			return null;
		}
		return center.addVector(pos.getX(), pos.getY(), pos.getZ());
	}
	public double getRadius() {
		return length;
	}
	public void setCenter(Vec3d center) {
		if (center != null) {
			this.center = center.subtract(pos.getX(), pos.getY(), pos.getZ());
		} else {
			this.center = center;
		}
		this.markDirty();
	}

	public TrackDirection getDirection() {
		return this.direction;
	}
	public void setDirection(TrackDirection dir) {
		this.direction = dir;
		this.markDirty();
	}
	
	
	public Vec3d getPlacementPosition() {
		return placementPosition.addVector(pos.getX(), pos.getY(), pos.getZ());
	}
	public void setPlacementPosition(Vec3d placementPosition) {
		this.placementPosition = placementPosition.subtract(pos.getX(), pos.getY(), pos.getZ());
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
		super.readFromNBT(nbt);
		
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		
		facing = EnumFacing.getFront(nbt.getByte("facing"));
		type = TrackItems.valueOf(nbt.getString("type"));
		
		switchState = SwitchState.values()[nbt.getInteger("switchState")];
		
		length = nbt.getInteger("length");
		rotationQuarter = nbt.getInteger("rotationQuarter");
		direction = TrackDirection.values()[nbt.getInteger("direction")];
		turnQuarters = nbt.getInteger("turnQuarters");
		
		this.drops = new ArrayList<ItemStack>();
		if (nbt.hasKey("drops")) {
			NBTTagCompound dropNBT = nbt.getCompoundTag("drops");
			int count = dropNBT.getInteger("count");
			for (int i = 0; i < count; i++) {
				drops.add(new ItemStack(dropNBT.getCompoundTag("drop_" + i)));
			}
		}
		switch(version) {
		case 0:
			// Add missing railbed setting
			nbt.setTag("railBed", new ItemStack(Blocks.GRAVEL).serializeNBT());
		case 1:
			// Convert positions to relative
			if (getNBTVec3d(nbt, "center") != null) {
				setNBTVec3d(nbt, "center", getNBTVec3d(nbt, "center").subtract(pos.getX(), pos.getY(), pos.getZ()));
			}
			setNBTVec3d(nbt, "placementPosition", getNBTVec3d(nbt, "placementPosition").subtract(pos.getX(), pos.getY(), pos.getZ()));
		case 2:
			// nothing yet...
		}
		
		railBed = new ItemStack(nbt.getCompoundTag("railBed"));
		center = getNBTVec3d(nbt, "center");
		placementPosition = getNBTVec3d(nbt, "placementPosition");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (facing == null) {
			// Something is wrong...
			ImmersiveRailroading.logger.error("INVALID TILE SAVE");
			return super.writeToNBT(nbt);
		}
		nbt.setByte("facing", (byte) facing.getIndex());
		nbt.setString("type", type.name());
		
		
		nbt.setInteger("switchState", switchState.ordinal());
		
		nbt.setInteger("length", length);
		nbt.setInteger("rotationQuarter", rotationQuarter);
		nbt.setInteger("direction", direction.ordinal());
		nbt.setInteger("turnQuarters", turnQuarters);
		
		if (drops != null && drops.size() != 0) {
			NBTTagCompound dropNBT = new NBTTagCompound();
			dropNBT.setInteger("count", drops.size());
			for (int i = 0; i < drops.size(); i++) {
				dropNBT.setTag("drop_" + i, drops.get(i).serializeNBT());
			}
			nbt.setTag("drops", dropNBT);
		}
		
		nbt.setTag("railBed", railBed.serializeNBT());
		setNBTVec3d(nbt, "center", center);
		setNBTVec3d(nbt, "placementPosition", placementPosition);
		
		return super.writeToNBT(nbt);
	}

	private RailInfo info;
	public RailInfo getRailRenderInfo() {
		if (!hasTileData && world.isRemote) {
			return null;
		}
		if (info == null) {
			info = new RailInfo(getPos(), getWorld(), getFacing().getOpposite(), getType(), getDirection(), getLength(), getRotationQuarter(), getTurnQuarters(), getPlacementPosition(), getRailBed(), false);
		}
		info.snowRenderFlagDirty = this.snowRenderFlagDirty;
		this.snowRenderFlagDirty = false;
		info.switchState = this.switchState;
		return info;
	}


	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
		this.markDirty();
	}
	public void spawnDrops() {
		if (!world.isRemote) {
			if (drops != null && drops.size() != 0) {
				for(ItemStack drop : drops) {
					world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), drop));
				}
				drops = new ArrayList<ItemStack>();
			}
		}
	}
}
