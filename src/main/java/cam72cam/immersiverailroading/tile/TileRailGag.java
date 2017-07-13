package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileRailGag extends TileEntity {
	public int originX;
	public int originY;
	public int originZ;
	public TrackType type;
	public float bbHeight = 0.125f;

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		originX = nbt.getInteger("originX");
		originY = nbt.getInteger("originY");
		originZ = nbt.getInteger("originZ");
		bbHeight = nbt.getFloat("bbHeight");
		type = TrackType.valueOf(nbt.getString("type"));

		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		nbt.setInteger("originX", originX);
		nbt.setInteger("originY", originY);
		nbt.setInteger("originZ", originZ);
		nbt.setFloat("bbHeight", bbHeight);
		nbt.setString("type", type.name());

		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {

		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);

		return new SPacketUpdateTileEntity(this.pos, 1, nbt);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
		this.readFromNBT(pkt.getNbtCompound());
		super.onDataPacket(net, pkt);
	}

	public TileRail getOrigin() {
		return (TileRail) world.getTileEntity(new BlockPos(originX, originY, originZ));
	}
}