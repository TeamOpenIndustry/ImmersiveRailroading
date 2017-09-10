package cam72cam.immersiverailroading.blocks;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
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
import cam72cam.immersiverailroading.library.TrackItems;
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
			ItemStack stack = new ItemStack(this, 1, tileEntity.getType().getMeta());
			ItemRail.setLength(stack, tileEntity.getLength());
			drops.add(stack);
			// todo drop components
			// todo drop snow?
		}
	}
	
	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items)
    {
		for (TrackItems i : TrackItems.values()) {
			items.add(new ItemStack(this, 1, i.getMeta()));
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
		return new ItemStack(this, 1, tileEntity.getType().getMeta());
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
