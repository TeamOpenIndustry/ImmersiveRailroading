package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import trackapi.lib.Util;

public class BlockUtil {
	public static boolean canBeReplaced(World world, BlockPos pos, boolean allowFlex) {
		
		if (world.isAirBlock(pos)) {
			return true;
		}
		
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
		if (block instanceof BlockLeaves) {
			return true;
		}
		if (block == IRBlocks.BLOCK_RAIL_PREVIEW) {
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
	
	public static boolean isIRRail(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockRailBase;
	}
	
	public static boolean isRail(World world, BlockPos pos) {
		return Util.getTileEntity(world, new Vec3d(pos), true) != null;
	}
	
	public static Rotation rotFromFacing(EnumFacing facing) {
		switch (facing) {
		case NORTH:
			return Rotation.NONE;
		case EAST:
			return Rotation.CLOCKWISE_90;
		case SOUTH:
			return Rotation.CLOCKWISE_180;
		case WEST:
			return Rotation.COUNTERCLOCKWISE_90;
		default:
			return Rotation.NONE;
		}
	}
	public static BlockPos rotateYaw(BlockPos pos, EnumFacing rotation) {
        return pos.rotate(rotFromFacing(rotation));
	}

}
