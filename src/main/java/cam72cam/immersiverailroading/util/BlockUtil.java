package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.RailBaseInstance;
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
import net.minecraftforge.common.IPlantable;
import trackapi.lib.Util;

public class BlockUtil {
	public static boolean canBeReplaced(World world, Vec3i pos, boolean allowFlex) {
		
		if (world.isAir(pos)) {
			return true;
		}
		
		Block block = world.getBlockInternal(pos);
		
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
		if (world.isBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW)) {
			return true;
		}
		if (allowFlex && isIRRail(world, pos)) {
			RailBaseInstance te = world.getBlockEntity(pos, RailBaseInstance.class);
			return te != null && te.isFlexible();
		}
		return false;
	}
	
	public static IBlockState itemToBlockState(cam72cam.mod.item.ItemStack stack) {
		Block block = Block.getBlockFromItem(stack.internal.getItem());
		@SuppressWarnings("deprecation")
		IBlockState gravelState = block.getStateFromMeta(stack.internal.getMetadata());
		if (block instanceof BlockLog ) {
			gravelState = gravelState.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Z);
		}
		return gravelState;
	}
	
	public static boolean isIRRail(World world, Vec3i pos) {
		return world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG) || world.isBlock(pos, IRBlocks.BLOCK_RAIL);
	}
	
	public static boolean isRail(World world, Vec3i pos) {
		return Util.getTileEntity(world.internal, new net.minecraft.util.math.Vec3d(pos.internal), true) != null;
	}
	
	public static Vec3i rotateYaw(Vec3i pos, Facing rotation) {
        return pos.rotate(Rotation.from(rotation));
	}

}
