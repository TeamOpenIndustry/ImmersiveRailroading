package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.SnowRenderUpdatePacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TileRailBase extends TileEntity {
	private BlockPos parent;
	private float height = 0;
	private int snowLayers = 0;
	public boolean snowRenderFlagDirty = true;
	protected boolean flexible = false;

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
		return parent;
	}
	public void setParent(BlockPos pos) {
		this.parent = pos;
		this.markDirty();
	}
	
	public boolean isFlexible() {
		return this.flexible;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		parent = getNBTBlockPos(nbt, "parent");
		height = nbt.getFloat("height");
		snowLayers = nbt.getInteger("snowLayers");
		flexible = nbt.getBoolean("flexible"); 
		super.readFromNBT(nbt);
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		setNBTBlockPos(nbt, "parent", parent);
		nbt.setFloat("height", height);
		nbt.setInteger("snowLayers", snowLayers);
		nbt.setBoolean("flexible", flexible);
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
	}
	
	protected final static void setNBTBlockPos(NBTTagCompound nbt, String key, BlockPos value) {
		if (value != null) {
			nbt.setLong(key, value.toLong());
		}
	}
	
	protected final static BlockPos getNBTBlockPos(NBTTagCompound nbt, String key) {
		 return nbt.hasKey(key) ? BlockPos.fromLong(nbt.getLong(key)) : null;
	}
	
	public Vec3d getCenterOfRail() {
		return new Vec3d(this.getPos()).addVector(0.5, 0, 0.5);
	}
	public TileRail getParentTile() {
		TileEntity te = world.getTileEntity(this.getParent());
		if (te instanceof TileRail) {
			return (TileRail)te ;
		}
		return null;
	}
}
