package cam72cam.immersiverailroading.tile;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.ParticleUtil;
import cam72cam.immersiverailroading.util.SwitchUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import trackapi.lib.ITrackTile;

public class TileRailBase extends SyncdTileEntity implements ITrackTile {
	public static TileRailBase get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileRailBase ? (TileRailBase) te : null;
	}
	
	private BlockPos parent;
	private double gauge = -1;
	private float height = 0;
	private int snowLayers = 0;
	protected boolean flexible = false;
	private boolean willBeReplaced = false; 
	private NBTTagCompound replaced;
	private boolean skipNextRefresh = false;
	public ItemStack railBedCache = null;
	
	public boolean isLoaded() {
		return !world.isRemote || hasTileData;
	}
	
	public void setGauge(double gauge) {
		this.gauge = gauge;
		this.markDirty();
	}
	public double getGauge() {
		return this.gauge;
	}
	public double getScale() {
		return this.gauge / Gauge.STANDARD.value();
	}

	public void setHeight(float height) {
		this.height = height;
		this.markDirty();
	}
	public float getHeight() {
		return this.height;
	}
	public int getSnowLayers() {
		return this.snowLayers;
	}
	public void setSnowLayers(int snowLayers) {
		this.snowLayers = snowLayers;
		this.markDirty();
	}
	public float getFullHeight() {
		return this.height + this.snowLayers / 8.0f;
	}
	
	public boolean handleSnowTick() {
		if (this.snowLayers < (Config.deepSnow ? 8 : 1)) {
			this.snowLayers += 1;
			this.markDirty();
			return true;
		}
		return false;
	}

	public BlockPos getParent() {
		if (parent == null) {
			ImmersiveRailroading.warn("Invalid block without parent");
			world.setBlockToAir(pos);
			return null;
		}
		return parent.add(pos);
	}
	public void setParent(BlockPos pos) {
		this.parent = pos.subtract(this.pos);
		this.markDirty();
	}
	
	public boolean isFlexible() {
		return this.flexible;
	}
	
	public ItemStack getRenderRailBed() {
		if (railBedCache == null) {
			TileRail pt = this.getParentTile();
			if (pt != null) {
				railBedCache = pt.getRailBed();
			}
		}
		return railBedCache;
	}
	
	public void writeUpdateNBT(NBTTagCompound nbt) {
		if (this.getRenderRailBed() != null) {
			nbt.setTag("renderBed", this.getRenderRailBed().serializeNBT());
		}
	}
	public void readUpdateNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("renderBed")) {
			this.railBedCache = new ItemStack(nbt.getCompoundTag("renderBed"));
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		
		
		height = nbt.getFloat("height");
		int oldSnowLayers = snowLayers;
		snowLayers = nbt.getInteger("snowLayers");
		if (oldSnowLayers > snowLayers && world != null && world.isRemote) {
			for (int i = 0; i < 30 * (oldSnowLayers); i ++) {
				ParticleUtil.spawnParticle(world, EnumParticleTypes.SNOWBALL, this.getCenterOfRail().addVector(Math.random() * 4-2, 1, Math.random() * 4-2));
			}
		}
		flexible = nbt.getBoolean("flexible");
		if (nbt.hasKey("replaced")) {
			replaced = nbt.getCompoundTag("replaced");
		}
		
		switch(version) {
		case 0:
			//NOP
		case 1:
			setNBTBlockPos(nbt, "parent", getNBTBlockPos(nbt, "parent").subtract(pos));
		case 2:
			nbt.setDouble("gauge", Gauge.STANDARD.value());
		case 3:
			// Nothing yet ...
		}
		parent = getNBTBlockPos(nbt, "parent");
		if (world != null && this.getParentTile() != null) {
			this.getParentTile().snowRenderFlagDirty = true;
		}
		gauge = nbt.getDouble("gauge");
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		setNBTBlockPos(nbt, "parent", parent);
		nbt.setFloat("height", height);
		nbt.setInteger("snowLayers", snowLayers);
		nbt.setBoolean("flexible", flexible);
		if (replaced != null) {
			nbt.setTag("replaced", replaced);
		}
		
		nbt.setDouble("gauge", gauge);
		
		nbt.setInteger("version", 3);
		
		
		return super.writeToNBT(nbt);
	}
	
	protected final static void setNBTBlockPos(NBTTagCompound nbt, String key, BlockPos value) {
		if (value != null) {
			nbt.setLong(key, value.toLong());
		}
	}
	protected final static void setNBTVec3d(NBTTagCompound nbt, String key, Vec3d value) {
		if (value != null) {
			nbt.setDouble(key + "X", value.x);
			nbt.setDouble(key + "Y", value.y);
			nbt.setDouble(key + "Z", value.z);
		}
	}
	
	protected final static BlockPos getNBTBlockPos(NBTTagCompound nbt, String key) {
		return nbt.hasKey(key) ? BlockPos.fromLong(nbt.getLong(key)) : null;
	}
	protected final static Vec3d getNBTVec3d(NBTTagCompound nbt, String key) {
		if (!nbt.hasKey(key + "X") || !nbt.hasKey(key + "Y") || !nbt.hasKey(key + "Z")) {
			return null;
		}
		return new Vec3d(nbt.getDouble(key + "X"),nbt.getDouble(key + "Y"),nbt.getDouble(key + "Z"));
	}
	
	public Vec3d getCenterOfRail() {
		return new Vec3d(this.getPos()).addVector(0.5, 0, 0.5);
	}
	public TileRail getParentTile() {
		if (this.getParent() == null) {
			return null;
		}
		return TileRail.get(world, this.getParent());
	}
	public void setReplaced(NBTTagCompound replaced) {
		this.replaced = replaced;
		this.markDirty();
	}
	public NBTTagCompound getReplaced() {
		return replaced;
	}
	
	public void setSkipNextRefresh() {
		this.skipNextRefresh = true;
	}
	
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		// This works around a hack where Chunk does a removeTileEntity directly after calling breakBlock
		// We have already removed the original TE and are replacing it with one which goes with a new block 
		if (this.skipNextRefresh ) {
			return false;
		}
		return super.shouldRefresh(world, pos, oldState, newState);
	}
	
	// Called before flex track replacement
	public void setWillBeReplaced(boolean value) {
		this.willBeReplaced = value;
	}
	// Called duing flex track replacement
	public boolean getWillBeReplaced() {
		return this.willBeReplaced;
	}

	public void cleanSnow() {
		int snow = this.getSnowLayers();
		if (snow > 1) {
			this.setSnowLayers(1);
			int snowDown = snow -1;
			for (int i = 1; i <= 3; i ++) {
				EnumFacing[] horiz = EnumFacing.HORIZONTALS;
				if (Math.random() > 0.5) {
					// Split between sides of the track
					ArrayUtils.reverse(horiz);
				}
				for (EnumFacing facing : horiz) {
					BlockPos ph = world.getPrecipitationHeight(pos.offset(facing, i));
					for (int j = 0; j < 3; j ++) {
						IBlockState state = world.getBlockState(ph);
						if (world.isAirBlock(ph) && !BlockUtil.isRail(world, ph.down())) {
							world.setBlockState(ph, Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snowDown));
							return;
						}
						if (world.getBlockState(ph).getBlock() == Blocks.SNOW) {
							ph = ph.up();
							continue;
						}
						if (world.getBlockState(ph).getBlock() == Blocks.SNOW_LAYER) {
							Integer currSnow = state.getValue(BlockSnow.LAYERS);
							if (currSnow == 8) {
								ph = ph.up();
								continue;
							}
							int toAdd = Math.min(8 - currSnow, snowDown);
							world.setBlockState(ph, state.withProperty(BlockSnow.LAYERS, currSnow + toAdd));
							snowDown -= toAdd;
							if (snowDown <= 0) {
								return;
							}
						}
						ph = ph.down();
					}
				}
			}
		}
	}
	
	@Override
	public double getTrackGauge() {
		return gauge;
	}
	
	@Override
	public double getTrackSlope() {
		TileRail parent = this.getParentTile();
		if (parent != null && parent.getType() == TrackItems.SLOPE) {
			return 1.0 / parent.getLength();
		}
		return 0;
	}
	
	@Override
	public Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion) {
		TileRail tile = this instanceof TileRail ? (TileRail) this : this.getParentTile();
		
		if (SwitchUtil.getSwitchState(tile, currentPosition) == SwitchState.STRAIGHT) {
			tile = tile.getParentTile();
		}
		
		double distanceMeters = motion.lengthVector();
		float rotationYaw = VecUtil.toYaw(motion);
		
		return MovementTrack.nextPosition(world, currentPosition, tile, rotationYaw, distanceMeters);
	}
}
