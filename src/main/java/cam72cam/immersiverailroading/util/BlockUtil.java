package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import trackapi.lib.Util;

public class BlockUtil {
	public static boolean canBeReplaced(World world, Vec3i pos, boolean allowFlex) {
		if (world.isReplacable(pos)) {
			return true;
		}
		
		if (world.isBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW)) {
			return true;
		}
		if (allowFlex && isIRRail(world, pos)) {
			RailBase te = world.getBlockEntity(pos, RailBase.class);
			return te != null && te.isFlexible();
		}
		return false;
	}

	public static boolean isIRRail(World world, Vec3i pos) {
		return world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG) || world.isBlock(pos, IRBlocks.BLOCK_RAIL);
	}
	
	public static boolean isRail(World world, Vec3i pos) {
		return Util.getTileEntity(world.internal, new net.minecraft.util.math.Vec3d(pos.internal), true) != null;
	}
}
