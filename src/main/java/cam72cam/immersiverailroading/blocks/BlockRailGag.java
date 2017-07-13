package cam72cam.immersiverailroading.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.TileRailGag;

public class BlockRailGag extends Block {

	public static final String name = "block_rail_gag";
	
	public BlockRailGag() {
		super(Material.IRON);
        setUnlocalizedName(ImmersiveRailroading.MODID + ":" + name);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, name));
		setHardness(1.0F);
		setSoundType(SoundType.METAL);
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return false;
	}
	
	@Override
	public int quantityDropped(Random par1Random) {
		return 0;
	}

	@Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return null;
	}
	
	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor){
		TileRailGag tileEntity = (TileRailGag) world.getTileEntity(pos);
		boolean isOriginAir = world.isAirBlock(new BlockPos(tileEntity.originX, tileEntity.originY, tileEntity.originZ));
		boolean isOnRealBlock = world.isSideSolid(pos.down(), EnumFacing.UP, false);
		if (isOriginAir || !isOnRealBlock) {
			this.breakBlock(tileEntity.getWorld(), pos, null);
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileRailGag();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}
}