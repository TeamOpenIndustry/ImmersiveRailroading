package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class SwitchUtil {
	public static SwitchState getSwitchState(TileRail rail) {
		if (rail == null) {
			return SwitchState.NONE;
		}
		if (!rail.isLoaded()) {
			return SwitchState.NONE;
		}
		TileRail parent = rail.getParentTile();
		if (parent == null || !parent.isLoaded()) {
			return SwitchState.NONE;
		}
		
		if (rail.getType() != TrackItems.TURN) {
			return SwitchState.NONE;
		}
		if (parent.getType() != TrackItems.SWITCH) {
			return SwitchState.NONE;
		}
		
		for (EnumFacing facing : EnumFacing.HORIZONTALS) {
			if (rail.getWorld().isBlockIndirectlyGettingPowered(new BlockPos(rail.getPlacementPosition()).offset(facing)) > 0) {
				return SwitchState.TURN;
			}
		}
		return SwitchState.STRAIGHT;
	}
}
