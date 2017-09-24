package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.SnowRenderUpdatePacket;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TileRailBase extends TileEntity {
	private BlockPos parent;
	private float height = 0;
	private int snowLayers = 0;
	public boolean snowRenderFlagDirty = true;
	protected boolean flexible = false;
	private boolean willBeReplaced = false; 
	private NBTTagCompound replaced;
	private boolean skipNextRefresh = false;
	protected boolean hasTileData = false;
	
	public boolean isLoaded() {
		return !world.isRemote || hasTileData;
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
		if (this.getParentTile() != null) {
			this.getParentTile().snowRenderFlagDirty = true;
		}
	}
	public float getFullHeight() {
		return this.height + this.snowLayers / 8.0f;
	}
	
	public void handleSnowTick() {
		if (this.snowLayers < (Config.deepSnow ? 8 : 1)) {
			this.snowLayers += 1;
			this.markDirty();
			ImmersiveRailroading.net.sendToDimension(new SnowRenderUpdatePacket(this.getWorld().provider.getDimension(), this.pos, snowLayers), this.getWorld().provider.getDimension());
		}
	}

	public BlockPos getParent() {
		return parent.add(pos);
	}
	public void setParent(BlockPos pos) {
		this.parent = pos.subtract(this.pos);
		this.markDirty();
	}
	
	public boolean isFlexible() {
		return this.flexible;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		
		
		height = nbt.getFloat("height");
		snowLayers = nbt.getInteger("snowLayers");
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
			// Nothing yet ...
		}
		
		parent = getNBTBlockPos(nbt, "parent");
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
		nbt.setInteger("version", 2);
		
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		
		return new SPacketUpdateTileEntity(this.getPos(), 1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
		super.onDataPacket(net, pkt);
		world.markBlockRangeForRenderUpdate(getPos(), getPos());
		snowRenderFlagDirty = true;
		hasTileData = true;
	}
	
	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		this.writeToNBT(tag);
		return tag;
	}
	
	@Override 
	public void handleUpdateTag(NBTTagCompound tag) {
		this.readFromNBT(tag);
		super.handleUpdateTag(tag);
		world.markBlockRangeForRenderUpdate(getPos(), getPos());
		snowRenderFlagDirty = true;
		hasTileData = true;
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
		TileEntity te = world.getTileEntity(this.getParent());
		if (te instanceof TileRail) {
			return (TileRail)te ;
		}
		return null;
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
}
