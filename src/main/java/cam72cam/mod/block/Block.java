package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.tile.IRedstoneProvider;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;

public abstract class Block {
    private final BlockSettings settings;
    public final net.minecraft.block.Block internal;
    public static final PropertyObject BLOCK_DATA = new PropertyObject("BLOCK_DATA");

    protected class BlockInternal extends net.minecraft.block.Block {
        public BlockInternal() {
            super(settings.material.internal);
            setHardness(settings.hardness);
            setSoundType(settings.material.soundType);
            setUnlocalizedName(settings.modID + ":" + settings.name);
            setRegistryName(new ResourceLocation(settings.modID, settings.name));
        }

        @Override
        public final void breakBlock(net.minecraft.world.World world, BlockPos pos, IBlockState state) {
            Block.this.onBreak(World.get(world), new Vec3i(pos));
            super.breakBlock(world, pos, state);
        }
        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            return Block.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ));
        }
        @Override
        public final net.minecraft.item.ItemStack getPickBlock(IBlockState state, RayTraceResult target, net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
            return Block.this.onPick(World.get(world), new Vec3i(pos)).internal;
        }
        @Override
        public void neighborChanged(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos) {
            this.onNeighborChange(worldIn, pos, fromPos);
        }
        @Override
        public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
            Block.this.onNeighborChange(World.get((net.minecraft.world.World) world), new Vec3i(pos), new Vec3i(neighbor));
        }

            /*
            Overrides
             */

        @Override
        public final boolean hasTileEntity(IBlockState state) {
            return settings.entity != null;
        }

        @Override
        public final net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, IBlockState state) {
            return settings.entity != null ? settings.entity.get() : null;
        }

        @Override
        @Nonnull
        protected BlockStateContainer createBlockState()
        {
            return settings.entity == null ? super.createBlockState() : new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty<?>[]{BLOCK_DATA});
        }

        @Override
        public IBlockState getExtendedState(IBlockState origState, IBlockAccess internal, BlockPos pos)
        {
            IExtendedBlockState state = (IExtendedBlockState)origState;
            Object te = World.get((net.minecraft.world.World) internal).getTileEntity(new Vec3i(pos), TileEntity.class);
            state = state.withProperty(BLOCK_DATA, te);
            return state;
        }

        @Override
        public final float getExplosionResistance(Entity exploder) {
            return settings.resistance;
        }


        @Override
        public final EnumBlockRenderType getRenderType(IBlockState state) {
            // TESR Renderer
            return EnumBlockRenderType.INVISIBLE;
        }


        @Override
        public final boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public final boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return new AxisAlignedBB(0, 0, 0, 1, 1, 1);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, Block.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 1.0F);
        }


        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            TileEntity entity = source.getTileEntity(pos);
            if (entity == null) {
                return new AxisAlignedBB(0, 0, 0, 1, 1, 1);
            }
            return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, Math.max(Block.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 0.25), 1.0F);
        }

        @Override
        public AxisAlignedBB getSelectedBoundingBox(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos)
        {
            return  getCollisionBoundingBox(state, worldIn, pos).expand(0, 0.1, 0).offset(pos);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return 0;
        }

        /*
         * Fence, glass override
         */
        @Override
        public boolean canBeConnectedTo(IBlockAccess internal, BlockPos pos, EnumFacing facing) {
            return settings.connectable;
        }
        @Deprecated
        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_)
        {
            if (settings.connectable) {
                return super.getBlockFaceShape(p_193383_1_, p_193383_2_, p_193383_3_, p_193383_4_);
            }

            if (p_193383_4_ == EnumFacing.UP) {
                // SNOW ONLY?
                return BlockFaceShape.SOLID;
            }
            return BlockFaceShape.UNDEFINED;
        }

        /* Redstone */

        @Override
        public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
        {
            if (settings.entity == null) {
                return 0;
            }
            World world = World.get((net.minecraft.world.World) blockAccess);
            net.minecraft.tileentity.TileEntity ent =  world.getTileEntity(new Vec3i(pos), net.minecraft.tileentity.TileEntity.class);
            if (ent instanceof IRedstoneProvider) {
                IRedstoneProvider provider = (IRedstoneProvider) ent;
                return provider.getRedstoneLevel();
            }
            return 0;
        }

        @Override
        public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
        {
            return this.getWeakPower(blockState, blockAccess, pos, side);
        }

        @Override
        public boolean canProvidePower(IBlockState state)
        {
            return true;
        }

            /* TODO
            @SideOnly(Side.CLIENT)
            public BlockRenderLayer getBlockLayer() {
                return BlockRenderLayer.CUTOUT_MIPPED;
            }
            */

    }

    public Block(BlockSettings settings) {
        this.settings = settings;

        internal = getBlock();
    }

    protected BlockInternal getBlock() {
        return new BlockInternal();
    }

    /*
    Helper Methods
     */

    public final String getName() {
        return internal.getRegistryName().getResourcePath();
    }

    /*
    Public functionality
     */

    public abstract void onBreak(World world, Vec3i pos);
    public abstract boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit);
    public abstract ItemStack onPick(World world, Vec3i pos);
    public abstract void onNeighborChange(World world, Vec3i pos, Vec3i neighbor);
    public double getHeight(World world, Vec3i pos) { return 1; }
}
