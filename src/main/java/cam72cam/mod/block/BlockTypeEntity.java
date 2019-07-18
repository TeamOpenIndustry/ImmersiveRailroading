package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;
import java.util.function.Function;

public abstract class BlockTypeEntity extends BlockType {
    public static final PropertyObject BLOCK_DATA = new PropertyObject("BLOCK_DATA");
    protected final Identifier id;

    public BlockTypeEntity(BlockSettings settings, Function<TileEntity, BlockEntity> constructData) {
        super(settings);
        id = new Identifier(settings.modID, settings.name);
        TileEntity.register(constructData, id);
        this.getTile().register(); // Idempotent
    }

    /*

    BlockType Implementation

    */

    protected class BlockTypeInternal extends BlockInternal {
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
        public IBlockState getExtendedState(IBlockState origState, IBlockAccess access, BlockPos pos)
        {
            // Try to get the "real" world object
            net.minecraft.tileentity.TileEntity teorig = access.getTileEntity(pos);
            if (teorig != null && teorig.hasWorld()) {

                Object te = World.get(teorig.getWorld()).getBlockEntity(new Vec3i(pos), cam72cam.mod.block.BlockEntity.class);
                if (te != null) {
                    IExtendedBlockState state = (IExtendedBlockState) origState;
                    state = state.withProperty(BLOCK_DATA, te);
                    return state;
                }
            }
            return super.getExtendedState(origState, access, pos);
        }
        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return super.getCollisionBoundingBox(state, source, pos);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, BlockTypeEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 1.0F);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return super.getBoundingBox(state, source, pos);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, Math.max(BlockTypeEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 0.25), 1.0F);
        }
    }

    protected BlockInternal getBlock() {
        return new BlockTypeInternal();
    }

    protected TileEntity getTile() {
        return new TileEntity(id);
    }


    private BlockEntity getInstance(World world, Vec3i pos) {
        TileEntity te = world.getTileEntity(pos, TileEntity.class);
        if (te != null) {
            return (BlockEntity) te.instance();
        }
        return null;
    }

    /*

    Add block data to normal block calls

     */

    @Override
    public final void onBreak(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            instance.onBreak();
        }
    }

    @Override
    public final boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onClick(player, hand, facing, hit);
        }
        return false;
    }

    @Override
    public final ItemStack onPick(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onPick();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public final void onNeighborChange(World world, Vec3i pos, Vec3i neighbor) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            instance.onNeighborChange(neighbor);
        }
    }

    public final double getHeight(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.getHeight();
        }
        return 1;
    }


}
