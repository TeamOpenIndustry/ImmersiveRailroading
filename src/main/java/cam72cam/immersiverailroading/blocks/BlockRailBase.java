package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockRailBase extends Block {
	public BlockRailBase(Material materialIn) {
		super(materialIn);
		setHardness(1.0F);
		setSoundType(SoundType.METAL);
		
		//setCreativeTab(ImmersiveRailroading.TrackTab);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		BlockPos parent = ((TileRailBase) world.getTileEntity(pos)).getParent();
		super.breakBlock(world, pos, state);

		if (parent != null) {
			world.destroyBlock(parent, true);
		}
	}
	
	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		this.onNeighborChange(worldIn, pos, fromPos);
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
		TileRailBase tileEntity = (TileRailBase) world.getTileEntity(pos);
		boolean isOriginAir = tileEntity.getParent() != null && world.isAirBlock(tileEntity.getParent());
		boolean isOnRealBlock = world.isSideSolid(pos.down(), EnumFacing.UP, false);
		if (isOriginAir || !isOnRealBlock) {
			//stupid IBlockAccess
			tileEntity.getWorld().destroyBlock(pos, true);
		}
		
		IBlockState up = world.getBlockState(pos.up());
		if (up.getBlock() == Blocks.SNOW_LAYER) {
			tileEntity.getWorld().setBlockToAir(pos.up());
			tileEntity.handleSnowTick();
		}
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}
	
	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileRailBase tileEntity = (TileRailBase) source.getTileEntity(pos);
		float height = 0.125F;
		if (tileEntity != null) {
			height = tileEntity.getFullHeight();
		}
		return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, height+0.1, 1.0F);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos)
	{
		return  getCollisionBoundingBox(state, worldIn, pos).expand(0, 0.1, 0).offset(pos);
	}
	
	@Override
	public int getMetaFromState(IBlockState state) {
		return 0;
	}
}
