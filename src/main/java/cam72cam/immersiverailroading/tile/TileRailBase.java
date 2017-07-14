package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileRailBase extends TileEntity {
	private BlockPos parent;

	public BlockPos getParent() {
		return parent;
	}
	public void setParent(BlockPos pos) {
		this.parent = pos;
		this.markDirty();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		parent = getNBTBlockPos(nbt, "parent");
		ImmersiveRailroading.logger.info(String.format("READ PARENT %s", parent));
		super.readFromNBT(nbt);
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		setNBTBlockPos(nbt, "parent", parent);
		ImmersiveRailroading.logger.info(String.format("WRITE PARENT %s", parent));
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
	}
	
	protected final static void setNBTBlockPos(NBTTagCompound nbt, String key, BlockPos value) {
		if (value != null) {
			nbt.setLong(key, value.toLong());
		}
	}
	
	protected final static BlockPos getNBTBlockPos(NBTTagCompound nbt, String key) {
		 return nbt.hasKey(key) ? BlockPos.fromLong(nbt.getLong(key)) : null;
	}
}
