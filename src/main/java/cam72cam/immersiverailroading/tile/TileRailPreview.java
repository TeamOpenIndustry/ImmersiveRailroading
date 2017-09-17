package cam72cam.immersiverailroading.tile;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileRailPreview extends TileEntity {

	private ItemStack item;
	float yawHead;
	float hitX;
	float hitY;
	float hitZ;
	public boolean hasTileData;

	public ItemStack getItem() {
		return this.item;
	}
	
	public void init(ItemStack item, float yawHead, float hitX, float hitY, float hitZ) {
		this.item = item;
		this.yawHead = yawHead;
		this.hitX = hitX;
		this.hitY = hitY;
		this.hitZ = hitZ;
		this.markDirty();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		item = new ItemStack(nbt.getCompoundTag("item"));
		yawHead = nbt.getFloat("yawHead");
		hitX = nbt.getFloat("hitX");
		hitX = nbt.getFloat("hitY");
		hitX = nbt.getFloat("hitZ");
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("item", item.serializeNBT());
		nbt.setFloat("yawHead", yawHead);
		nbt.setFloat("hitX", hitX);
		nbt.setFloat("hitY", hitY);
		nbt.setFloat("hitZ", hitZ);
		
		return super.writeToNBT(nbt);
	}
	
	public RailInfo getRailRenderInfo() {
		if (hasTileData) {
			return new RailInfo(item, world, yawHead, pos, hitX, hitY, hitZ);
		}
		return null;
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
		hasTileData = true;
	}
}
