package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.world.World;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IPlantable;
import trackapi.lib.Util;

public class BlockUtil {
	public static boolean canBeReplaced(World world, Vec3i pos, boolean allowFlex) {
		
		if (world.isAir(pos)) {
			return true;
		}
		
		Block block = world.getBlock(pos);
		
		if (block == null) {
			return true;
		}
		if (block.isReplaceable(world.internal, pos.internal)) {
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
			TileRailBase te = world.getTileEntity(pos, TileRailBase.class);
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
	
	public static boolean isIRRail(World world, Vec3i pos) {
		return world.getBlock(pos) instanceof BlockRailBase;
	}
	
	public static boolean isRail(World world, Vec3i pos) {
		return Util.getTileEntity(world.internal, new net.minecraft.util.math.Vec3d(pos.internal), true) != null;
	}
	
	public static Vec3i rotateYaw(Vec3i pos, Facing rotation) {
        return pos.rotate(Rotation.from(rotation));
	}

}
