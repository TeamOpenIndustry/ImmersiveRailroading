package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

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
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
		TileRailBase tileEntity = (TileRailBase) world.getTileEntity(pos);
		boolean isOriginAir = tileEntity.getParent() != null && world.isAirBlock(tileEntity.getParent());
		boolean isOnRealBlock = world.isSideSolid(pos.down(), EnumFacing.UP, false);
		if (isOriginAir || !isOnRealBlock) {
			ImmersiveRailroading.logger.info("BLOCK BROKEN");
			//stupid IBlockAccess
			tileEntity.getWorld().destroyBlock(pos, true);
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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
	}
	
	@Override
	public int getMetaFromState(IBlockState state) {
		return 0;
	}
}
