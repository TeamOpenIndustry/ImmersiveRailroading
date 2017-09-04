package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import net.minecraft.util.EnumFacing;

public class SwitchUtil {
	public static SwitchState getSwitchState(TileRail rail) {
		TileRail parent = rail.getParentTile();
		
		if (rail.getType() != TrackItems.TURN) {
			return SwitchState.NONE;
		}
		if (parent.getType() != TrackItems.SWITCH) {
			return SwitchState.NONE;
		}
		
		for (EnumFacing facing : EnumFacing.HORIZONTALS) {
			if (rail.getWorld().isBlockIndirectlyGettingPowered(rail.getPos().offset(facing)) > 0) {
				return SwitchState.TURN;
			}
		}
		return SwitchState.STRAIGHT;
	}
}
