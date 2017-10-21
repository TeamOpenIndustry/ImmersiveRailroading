package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.util.SwitchUtil;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
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
		TileRailBase te = (TileRailBase) world.getTileEntity(pos);
		
		if (te instanceof TileRail) {
			((TileRail) te).spawnDrops();
		}
		
		breakParentIfExists(te);
		super.breakBlock(world, pos, state);
	}
	
	public static void breakParentIfExists(TileRailBase te) {
		BlockPos parent = te.getParent();
		if (parent != null && !te.getWillBeReplaced()) {
			if (te.getWorld().getBlockState(parent).getBlock() instanceof BlockRail) {
				if (te.getParentTile() != null) {
					te.getParentTile().spawnDrops();
				}
				te.getWorld().setBlockToAir(parent);
			}
		}
	}
	
	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		this.onNeighborChange(worldIn, pos, fromPos);
	}
	
	public static boolean tryBreakRail(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileRailBase) {
			TileRailBase rail = (TileRailBase)te;
			if (rail.getReplaced() != null) {
				// new object here is important
				TileRailGag newGag = new TileRailGag();
				newGag.readFromNBT(rail.getReplaced());
				
				// Only do replacement if parent still exists
				if (rail.getWorld().getTileEntity(newGag.getParent()) != null) {
					rail.getWorld().setTileEntity(pos, newGag);
					newGag.markDirty();
					breakParentIfExists(rail);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
		TileRailBase tileEntity = (TileRailBase) world.getTileEntity(pos);
		if (tileEntity.getWorld().isRemote) {
			return;
		}
		boolean isOriginAir = tileEntity.getParentTile() == null || tileEntity.getParentTile().getParentTile() == null;
		boolean isOnRealBlock = world.isSideSolid(pos.down(), EnumFacing.UP, false);
		if (isOriginAir || !isOnRealBlock) {
			if (tryBreakRail(world, pos)) { 
				tileEntity.getWorld().destroyBlock(pos, true);
			}
			return;
		}
		
		IBlockState up = world.getBlockState(pos.up());
		if (up.getBlock() == Blocks.SNOW_LAYER) {
			tileEntity.getWorld().setBlockToAir(pos.up());
			tileEntity.handleSnowTick();
		}
		if (tileEntity.getParentTile() != null && tileEntity.getParentTile().getParentTile() != null) {
			SwitchState state = SwitchUtil.getSwitchState(tileEntity.getParentTile());
			if (state != SwitchState.NONE) {
				tileEntity.getParentTile().setSwitchState(state);
			}
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

	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileRailBase tileEntity = (TileRailBase) source.getTileEntity(pos);
		float height = 0.125F;
		if (tileEntity != null) {
			height = tileEntity.getFullHeight();
		}
		return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F);
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
	
	/*
	 * Fence, glass override
	 */
	@Override
    public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
		return false;
	}
	@Deprecated
	@Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_)
    {
		if (p_193383_4_ == EnumFacing.UP) {
			// SNOW ONLY?
			return BlockFaceShape.SOLID;
		}
        return BlockFaceShape.UNDEFINED;
    }
}
