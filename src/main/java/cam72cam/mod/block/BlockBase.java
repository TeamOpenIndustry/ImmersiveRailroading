package cam72cam.mod.block;

import cam72cam.mod.*;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.tile.IRedstoneProvider;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;

public abstract class BlockBase extends Block {
    private final BlockSettings settings;

    public BlockBase(BlockSettings settings) {
        super(settings.material.internal);
        setHardness(settings.hardness);
        setSoundType(settings.material.soundType);

        setUnlocalizedName(settings.modID + ":" + settings.name);
        setRegistryName(new ResourceLocation(settings.modID, settings.name));
        this.settings = settings;
    }

    /*
    Helper Methods
     */

    public final String getName() {
        return this.getRegistryName().getResourcePath();
    }

    /*
    Public functionality
     */

    @Override
    public final void breakBlock(net.minecraft.world.World world, BlockPos pos, IBlockState state) {
        this.onBreak(new World(world), new Vec3i(pos));
        super.breakBlock(world, pos, state);
    }
    public abstract void onBreak(World world, Vec3i pos);

    @Override
    public final boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return this.onClick(new World(world), new Vec3i(pos), new Player(player), Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ));
    }
    public abstract boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit);

    @Override
    public final net.minecraft.item.ItemStack getPickBlock(IBlockState state, RayTraceResult target, net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
        return this.onPick(new World(world), new Vec3i(pos)).internal;
    }
    public abstract ItemStack onPick(World world, Vec3i pos);

    @Override
    public void neighborChanged(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        this.onNeighborChange(worldIn, pos, fromPos);
    }
    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
        this.onNeighborChange(new World((net.minecraft.world.World) world), new Vec3i(pos), new Vec3i(neighbor));
    }
    public abstract void onNeighborChange(World world, Vec3i pos, Vec3i neighbor);

    public double getHeight(World world, Vec3i pos) {
        return 1;
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
        return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, this.getHeight(new World((net.minecraft.world.World) source), new Vec3i(pos)), 1.0F);
    }


    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, this.getHeight(new World((net.minecraft.world.World) source), new Vec3i(pos)), 1.0F);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos)
    {
        return  getCollisionBoundingBox(state, worldIn, pos);//.expand(0, 0.1, 0).offset(pos);
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
        World world = new World((net.minecraft.world.World) blockAccess);
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
