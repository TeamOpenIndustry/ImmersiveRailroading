package cam72cam.immersiverailroading.blocks;

import java.util.Random;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.tile.TileRail;

public class BlockRail extends BlockRailBase {
	public static final String NAME = "block_rail";
	public static final PropertyEnum<TrackType> TRACK_TYPE = PropertyEnum.create("track_type", TrackType.class);
	public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST);

	public BlockRail() {
		super(Material.IRON);
		
        setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
		
        // Do we need this?
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TRACK_TYPE, TrackType.STRAIGHT_SMALL));
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		drops.add(new ItemStack(this, 1, state.getValue(TRACK_TYPE).getMeta()));
	}
	
	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items)
    {
		for (TrackItems i : TrackItems.values()) {
			items.add(new ItemStack(this, 1, i.getMeta()));
		}
    }

	@Override
    public BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[]{FACING, TRACK_TYPE});
    }
	
	@Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
		TrackDirection dir = (placer.getRotationYawHead() % 90 < 45) ? TrackDirection.RIGHT : TrackDirection.LEFT;
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing()).withProperty(TRACK_TYPE, TrackType.fromMeta(meta, dir));
    }
	
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    	//TODO safe block access
    	TileRail tr = (TileRail)world.getTileEntity(pos);
		//ImmersiveRailroading.logger.info(String.format("GET STATE !!!!! %s", state.getValue(IS_VISIBLE)));
		return state.withProperty(BlockRail.FACING, tr.getFacing()).withProperty(BlockRail.TRACK_TYPE, tr.getType());
    }

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return true;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return new ItemStack(this, 0, state.getValue(TRACK_TYPE).getMeta());
	}

	@Override
	public int quantityDropped(Random random) {
		return 1;
	}
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		// TESR Renderer
		return EnumBlockRenderType.INVISIBLE;
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
