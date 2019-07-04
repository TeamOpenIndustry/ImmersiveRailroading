package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.BuilderSwitch;
import cam72cam.immersiverailroading.track.IIterableTrack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SwitchUtil {
	public static SwitchState getSwitchState(TileRail rail) {
		return getSwitchState(rail, null);
	}

	public static SwitchState getSwitchState(TileRail rail, Vec3d position) {
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
		
		if (rail.info.settings.type != TrackItems.TURN && rail.info.settings.type != TrackItems.CUSTOM) {
			return SwitchState.NONE;
		}
		if (parent.info.settings.type != TrackItems.SWITCH) {
			return SwitchState.NONE;
		}

		if (position != null && parent.info != null) {
			IIterableTrack switchBuilder = (IIterableTrack) parent.info.getBuilder();
			IIterableTrack turnBuilder = (IIterableTrack) rail.info.getBuilder();
			boolean isOnStraight = switchBuilder.isOnTrack(parent.info, position);
			boolean isOnTurn = turnBuilder.isOnTrack(rail.info, position);

			if (isOnStraight && !isOnTurn) {
				return SwitchState.STRAIGHT;
			}
			if (!isOnStraight && isOnTurn) {
				return SwitchState.NONE;
			}
		}

		if (parent.isSwitchForced()) {
			return parent.info.switchForced;
		}

		if (isRailPowered(rail)) {
			return SwitchState.TURN;
		}

		return SwitchState.STRAIGHT;
	}

	public static boolean isRailPowered(TileRail rail) {
		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition;
		double horiz = rail.info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && rail.info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		int scale = (int)Math.round(horiz);
		for (int x = -scale; x <= scale; x++) {
			for (int z = -scale; z <= scale; z++) {
				BlockPos gagPos = new BlockPos(redstoneOrigin.add(new Vec3d(x, 0, z)));
				TileRailBase gagRail = TileRailBase.get(rail.getWorld(), gagPos);
				if (gagRail != null && (rail.getPos().equals(gagRail.getParent()) || gagRail.getReplaced() != null)) {
					if (rail.getWorld().isBlockIndirectlyGettingPowered(gagPos) > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
