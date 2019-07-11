package cam72cam.mod.block.tile;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Identifier;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.function.Function;

public class TileEntity extends net.minecraft.tileentity.TileEntity {
    public World world;
    public Vec3i pos;
    public boolean hasTileData;
    private String instanceId;
    private BlockEntity instance;

    /*
    Tile registration
    */

    private static final Map<String, Function<TileEntity, BlockEntity>> registry = HashBiMap.create();
    private TagCompound deferredLoad;

    public TileEntity() {
        // Forge reflection
        super();
    }

    public TileEntity(Identifier id) {
        this();
        instanceId = id.toString();
    }

    public static void register(Function<TileEntity, BlockEntity> instance, Identifier id) {
        registry.put(id.toString(), instance);
    }

    public final void register() {
        ResourceLocation currentName = TileEntity.getKey(this.getClass());
        if (currentName != null) {
            if (!currentName.toString().equals(getName().toString())) {
                throw new RuntimeException("Duplicate TileEntity registration with different name: " + currentName + " : " + getName());
            }
            // TODO log a warning here?
            return;
        }
        net.minecraft.tileentity.TileEntity.register(this.getName().internal.toString(), this.getClass());
    }

    public Identifier getName() {
        //TODO use MODID
        return new Identifier("notreallyamodthismightbreak", "hack");
    }


    /*
    Standard Tile function overrides
    */

    @Override
    public void setWorld(net.minecraft.world.World world) {
        super.setWorld(world);
        this.world = World.get(world);
    }
    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        this.pos = new Vec3i(pos);
    }


    @Override
    public final void readFromNBT(NBTTagCompound compound) {
        load(new TagCompound(compound));
    }
    @Override
    public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
        return compound;
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



    @Override
    public void markDirty() {
        super.markDirty();
        if (world.isServer) {
            world.internal.notifyBlockUpdate(getPos(), world.internal.getBlockState(getPos()), world.internal.getBlockState(getPos()), 1 + 2 + 8);
            world.internal.notifyNeighborsOfStateChange(pos.internal, this.getBlockType(), true);
        }
    }

    /*
    Wrapped functionality
    */

    public void setWorld(World world) {
        super.setWorld(world.internal);
    }
    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }

    public void load(TagCompound data) {
        super.readFromNBT(data.internal);
        pos = new Vec3i(super.pos);

        if (instanceId == null) {
            // If this fails something is really wrong
            instanceId = data.getString("instanceId");
            if (instanceId == null) {
                throw new RuntimeException("Unable to load instanceid with " + data.toString());
            }
        }

        if (instance() != null) {
            instance().load(data);
        } else {
            deferredLoad = data;
        }
    }
    public void save(TagCompound data) {
        super.writeToNBT(data.internal);
        data.setString("instanceId", instanceId);
        if (instance() != null) {
            instance().save(data);
        }
    }

    public void writeUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().writeUpdate(nbt);
        }
    }
    public void readUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().readUpdate(nbt);
        }
    }

    /*
    New Functionality
    */

    public boolean isLoaded() {
        return this.hasWorld() && (world.isServer || hasTileData);
    }

    // TODO render system?
    public boolean updateRerender() {
        return false;
    }

    public BlockEntity instance() {
        if (this.instance == null) {
            if (isLoaded()) {
                this.instance = registry.get(this.instanceId).apply(this);
                if (deferredLoad != null) {
                    this.instance.load(deferredLoad);
                }
                this.deferredLoad = null;
            }
        }
        return this.instance;
    }
}
