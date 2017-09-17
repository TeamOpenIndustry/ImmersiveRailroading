package cam72cam.immersiverailroading.blocks;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRail;
import cam72cam.immersiverailroading.tile.TileRail;

public class BlockRail extends BlockRailBase {
	public static final String NAME = "block_rail";

	public BlockRail() {
		super(Material.IRON);
		
        setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		TileRail tileEntity = (TileRail) world.getTileEntity(pos);
		if (tileEntity != null && tileEntity.getType() != null) {
			ItemStack stack = new ItemStack(this, 1);
			drops.add(stack);
			// todo drop components
			// todo drop snow?
		}
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return true;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		TileRail tileEntity = (TileRail) world.getTileEntity(pos);
		if (tileEntity == null) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(this, 1);
		ItemRail.setType(stack, tileEntity.getType());
		ItemRail.setLength(stack, tileEntity.getLength());
		ItemRail.setQuarters(stack, tileEntity.getTurnQuarters());
		//ItemRail.setPosType(stack, )
		ItemRail.setBed(stack, tileEntity.getRailBed());
		//ItemRail.setPreview(stack, )
		
		return stack;
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
		return new TileRail();
	}
}
