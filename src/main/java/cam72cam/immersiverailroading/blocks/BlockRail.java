package cam72cam.immersiverailroading.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.tile.TileRail;

public class BlockRail extends Block {
	public static final String NAME = "block_rail";
	public static final PropertyEnum<TrackType> TRACK_TYPE = PropertyEnum.create("track_type", TrackType.class);
	public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST);

	public BlockRail() {
		super(Material.IRON);
		setHardness(1.0F);
		setSoundType(SoundType.METAL);
		
        setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        
		//setCreativeTab(ImmersiveRailroading.TrackTab);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
		
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TRACK_TYPE, TRACK_TYPE.getAllowedValues().iterator().next()));
	}

	@Override
    public BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[]{FACING, TRACK_TYPE});
    }
	
	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items)
    {
		for (TrackItems i : TrackItems.values()) {
			items.add(new ItemStack(this, 0, i.getMeta()));
		}
    }
	
	@Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(TRACK_TYPE, TrackType.fromMeta(meta, TrackDirection.LEFT));
    }
	
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    	TileRail tr = (TileRail)world.getTileEntity(pos);
		return state.withProperty(BlockRail.FACING, tr.getFacing()).withProperty(BlockRail.TRACK_TYPE, tr.getType());
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState();
    }
    
    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }
	
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return true;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileRail tileEntity = (TileRail) world.getTileEntity(pos);
		if (tileEntity != null && tileEntity.idDrop != null) {
			return new ItemStack(tileEntity.idDrop);
		}
		return null;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}
	
	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
		TileRail tileEntity = (TileRail) world.getTileEntity(pos);
		boolean isOriginAir = world.isAirBlock(new BlockPos(tileEntity.linkedX, tileEntity.linkedY, tileEntity.linkedZ));
		boolean isOnRealBlock = world.isSideSolid(pos.down(), EnumFacing.UP, false);
		if (isOriginAir || !isOnRealBlock) {
			this.breakBlock(tileEntity.getWorld(), pos, null);
		}
	}
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
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
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		ImmersiveRailroading.logger.info("SUPER TILE");
		TileRail t = new TileRail();
		t.setType(state.getValue(BlockRail.TRACK_TYPE));
        t.setFacing(state.getValue(BlockRail.FACING));
		return t;
	}
}
