package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

public class BlockUtil {
	public static boolean canBeReplaced(World world, BlockPos pos, boolean allowFlex) {
		Block block = world.getBlockState(pos).getBlock();
		
		if (block == null) {
			return true;
		}
		if (block.isReplaceable(world, pos)) {
			return true;
		}
		if (block instanceof IGrowable && !(block instanceof BlockGrass)) {
			return true;
		}
		if (block instanceof IPlantable) {
			return true;
		}
		if (block instanceof BlockLiquid) {
			return true;
		}
		if (block instanceof BlockSnow) {
			return true;
		}
		if (block == ImmersiveRailroading.BLOCK_RAIL_PREVIEW) {
			return true;
		}
		if (allowFlex && block instanceof BlockRailBase) {
			TileRailBase te = TileRailBase.get(world, pos);
			return te != null && te.isFlexible();
		}
		return false;
	}
	
	public static IBlockState itemToBlockState(ItemStack stack) {
		Block block = Block.getBlockFromItem(stack.getItem());
		@SuppressWarnings("deprecation")
		IBlockState gravelState = block.getStateFromMeta(stack.getMetadata());
		if (block instanceof BlockLog ) {
			gravelState = gravelState.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Z);
		}
		return gravelState;
	}
	
	public static boolean isRail(IBlockState state) {
		return state.getBlock() == ImmersiveRailroading.BLOCK_RAIL || state.getBlock() == ImmersiveRailroading.BLOCK_RAIL_GAG;
	}
}
