package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;
import java.util.function.Function;

public abstract class BlockEntity<T extends BlockEntityInstance> extends Block {
    public static final PropertyObject BLOCK_DATA = new PropertyObject("BLOCK_DATA");
    private final Function<Internal, T> instance;

    public BlockEntity(BlockSettings settings, Function<Internal, T> constructData) {
        super(settings);
        this.instance = constructData;
    }

    /*

    Tickable

     */

    public static class Tickable<S extends BlockEntityInstance.Tickable> extends BlockEntity<S> {
        public Tickable(BlockSettings settings, Function<BlockEntity<S>.Internal, S> constructData) {
            super(settings, constructData);
        }

        protected class Internal extends BlockEntity.Internal implements ITickable {

            @Override
            public void update() {
                BlockEntityInstance.Tickable z = (BlockEntityInstance.Tickable) instance();
                z.update();
            }
        }

        protected Internal getTile() {
            return new Internal();
        }
    }

    /*

    Block Implementation

    */

    protected class BlockEntityInternal extends BlockInternal {
        @Override
        public final boolean hasTileEntity(IBlockState state) {
            return true;
        }

        @Override
        public final net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, IBlockState state) {
            return getTile();
        }

        @Override
        @Nonnull
        protected BlockStateContainer createBlockState()
        {
            return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty<?>[]{BLOCK_DATA});
        }

        @Override
        public IBlockState getExtendedState(IBlockState origState, IBlockAccess internal, BlockPos pos)
        {
            Object te = World.get((net.minecraft.world.World) internal).getBlockEntity(new Vec3i(pos), BlockEntityInstance.class);
            if (te != null) {
                IExtendedBlockState state = (IExtendedBlockState) origState;
                state = state.withProperty(BLOCK_DATA, te);
                return state;
            }
            return super.getExtendedState(origState, internal, pos);
        }
        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return super.getCollisionBoundingBox(state, source, pos);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, BlockEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 1.0F);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return super.getBoundingBox(state, source, pos);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, Math.max(BlockEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 0.25), 1.0F);
        }
    }

    protected BlockInternal getBlock() {
        return new BlockEntityInternal();
    }

    /*

    Block Entity Tile Implementation

     */

    public class Internal extends net.minecraft.tileentity.TileEntity {
        public World world;
        public Vec3i pos;
        public boolean hasTileData;
        private BlockEntityInstance instance;

        @Override
        public final void readFromNBT(NBTTagCompound compound) {
            load(new TagCompound(compound));
        }
        @Override
        public final NBTTagCompound writeToNBT(NBTTagCompound compound) {
            save(new TagCompound(compound));
            return compound;
        }

        public void load(TagCompound data) {
            super.readFromNBT(data.internal);
            pos = new Vec3i(super.pos);
        }
        public void save(TagCompound data) {
            super.writeToNBT(data.internal);
        }

        @Override
        public void setWorld(net.minecraft.world.World world) {
            super.setWorld(world);
            this.world = World.get(world);
        }
        public void setWorld(World world) {
            setWorld(world.internal);
        }
        @Override
        public void setPos(BlockPos pos) {
            this.pos = new Vec3i(pos);
            super.setPos(pos);
        }
        public void setPos(Vec3i pos) {
            super.setPos(pos.internal);
        }

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

        public T instance() {
            if (this.instance == null) {
                if (isLoaded()) {
                    this.instance = BlockEntity.this.instance.apply(this);
                }
            }
            return (T) this.instance;
        }
    }

    protected Internal getTile() {
        return new Internal();
    }


    private T getInstance(World world, Vec3i pos) {
        Internal te = world.getTileEntity(pos, Internal.class);
        if (te != null) {
            return te.instance();
        }
        return null;
    }

    /*

    Add block data to normal block calls

     */

    @Override
    public final void onBreak(World world, Vec3i pos) {
        T instance = getInstance(world, pos);
        if (instance != null) {
            instance.onBreak();
        }
    }

    @Override
    public final boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit) {
        T instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onClick(player, hand, facing, hit);
        }
        return false;
    }

    @Override
    public final ItemStack onPick(World world, Vec3i pos) {
        T instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onPick();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public final void onNeighborChange(World world, Vec3i pos, Vec3i neighbor) {
        T instance = getInstance(world, pos);
        if (instance != null) {
            instance.onNeighborChange(neighbor);
        }
    }

    public final double getHeight(World world, Vec3i pos) {
        T instance = getInstance(world, pos);
        if (instance != null) {
            return instance.getHeight();
        }
        return 1;
    }


}
