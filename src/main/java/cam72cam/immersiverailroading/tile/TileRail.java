package cam72cam.immersiverailroading.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;

public class TileRail extends TileRailBase {
	public static TileRail get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileRail ? (TileRail) te : null;
	}
	
	private EnumFacing facing;
	private TrackItems type;
	private ItemStack railBed;
	
	private Gauge gauge;
	
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
		return new AxisAlignedBB(-length, -length, -length, length, length, length).offset(pos);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return Math.pow(8*32, 2);
	}

	public EnumFacing getFacing() {
		if (facing == EnumFacing.DOWN) {
			return EnumFacing.NORTH;
		}
		return facing;
	}
	public void setFacing(EnumFacing value) {
		this.facing = value;
	}
	
	public TrackItems getType() {
		return this.type;
	}
	public void setType(TrackItems value) {
		this.type = value;
	}
	public ItemStack getRailBed() {
		return this.railBed;
	}
	public void setRailBed(ItemStack railBed) {
		this.railBed = railBed;
	}
	
	public void setGauge(Gauge gauge) {
		this.gauge = gauge;
	}
	public Gauge getGauge() {
		return this.gauge;
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
	}

	public TrackDirection getDirection() {
		return this.direction;
	}
	public void setDirection(TrackDirection dir) {
		this.direction = dir;
	}
	
	
	public Vec3d getPlacementPosition() {
		return placementPosition.addVector(pos.getX(), pos.getY(), pos.getZ());
	}
	public void setPlacementPosition(Vec3d placementPosition) {
		this.placementPosition = placementPosition.subtract(pos.getX(), pos.getY(), pos.getZ());
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
	}

	public int getRotationQuarter() {
		return this.rotationQuarter;
	}
	public void setRotationQuarter(int val) {
		this.rotationQuarter = val;
	}

	public int getTurnQuarters() {
		return this.turnQuarters;
	}
	public void setTurnQuarters(int quarters) {
		this.turnQuarters = quarters;
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
			nbt.setDouble("gauge", Gauge.STANDARD.value());
		case 3:
			// nothing yet...
		}
		
		railBed = new ItemStack(nbt.getCompoundTag("railBed"));
		center = getNBTVec3d(nbt, "center");
		placementPosition = getNBTVec3d(nbt, "placementPosition");
		gauge = Gauge.from(nbt.getDouble("gauge"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (facing == null) {
			// Something is wrong...
			ImmersiveRailroading.error("INVALID TILE SAVE");
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
		nbt.setDouble("gauge", gauge.value());
		
		return super.writeToNBT(nbt);
	}

	private RailInfo info;
	public RailInfo getRailRenderInfo() {
		if (!hasTileData && world.isRemote) {
			return null;
		}
		if (info == null) {
			info = new RailInfo(getPos(), getWorld(), getFacing().getOpposite(), getType(), getDirection(), getLength(), getRotationQuarter(), getTurnQuarters(), getGauge(), getPlacementPosition(), getRailBed(), ItemStack.EMPTY);
		}
		info.snowRenderFlagDirty = this.snowRenderFlagDirty;
		this.snowRenderFlagDirty = false;
		info.switchState = this.switchState;
		return info;
	}


	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
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

	public double percentFloating() {
		RailInfo buildInfo = new RailInfo(getPos(), getWorld(), getFacing().getOpposite(), getType(), getDirection(), getLength(), getRotationQuarter(), getTurnQuarters(), getGauge(), getPlacementPosition(), getRailBed(), ItemStack.EMPTY);
		
		List<TrackBase> tracks = buildInfo.getBuilder(pos).getTracksForRender();
		double floating = 0;
		
		for (TrackBase track : tracks) {
			if (!world.isSideSolid(track.getPos().down(), EnumFacing.UP, false)) {
				floating += 1.0 / tracks.size();
			}
		}
		return floating;
	}
}
