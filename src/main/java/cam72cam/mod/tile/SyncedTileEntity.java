package cam72cam.mod.tile;

import cam72cam.mod.util.TagCompound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

public class SyncedTileEntity extends TileEntity {
    public boolean hasTileData;

    public boolean isLoaded() {
        return this.hasWorld() && (world.isServer || hasTileData);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world.isServer) {
            world.internal.notifyBlockUpdate(getPos(), world.internal.getBlockState(getPos()), world.internal.getBlockState(getPos()), 1 + 2 + 8);
            world.internal.notifyNeighborsOfStateChange(pos.internal, this.getBlockType(), true);
        }
    }

    public void writeUpdate(TagCompound nbt) {
    }
    public void readUpdate(TagCompound nbt) {
    }


    @Override
    public final SPacketUpdateTileEntity getUpdatePacket() {
        TagCompound nbt = new TagCompound();
        this.writeToNBT(nbt.internal);
        this.writeUpdate(nbt);

        return new SPacketUpdateTileEntity(this.getPos(), 1, nbt.internal);
    }

    @Override
    public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
        this.readUpdate(new TagCompound(pkt.getNbtCompound()));
        super.onDataPacket(net, pkt);
        if (updateRerender()) {
            world.internal.markBlockRangeForRenderUpdate(getPos(), getPos());
        }
        hasTileData = true;
    }

    @Override
    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        if (this.isLoaded()) {
            this.writeToNBT(tag);
            this.writeUpdate(new TagCompound(tag));
        }
        return tag;
    }

    public boolean updateRerender() {
        return false;
    }

    @Override
    public final void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
        this.readUpdate(new TagCompound(tag));
        super.handleUpdateTag(tag);
        if (updateRerender()) {
            world.internal.markBlockRangeForRenderUpdate(getPos(), getPos());
        }
        hasTileData = true;
    }

}
