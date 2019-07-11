package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class SwitchUtil {
	public static SwitchState getSwitchState(Rail rail) {
		return getSwitchState(rail, null);
	}

	public static SwitchState getSwitchState(Rail rail, Vec3d position) {
		if (rail == null) {
			return SwitchState.NONE;
		}
		if (!rail.isLoaded()) {
			return SwitchState.NONE;
		}
		Rail parent = rail.getParentTile();
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

	public static boolean isRailPowered(Rail rail) {
		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition;
		double horiz = rail.info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && rail.info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		int scale = (int)Math.round(horiz);
		for (int x = -scale; x <= scale; x++) {
			for (int z = -scale; z <= scale; z++) {
				Vec3i gagPos = new Vec3i(redstoneOrigin.add(new Vec3d(x, 0, z)));
				RailBase gagRail = rail.world.getBlockEntity(gagPos, RailBase.class);
				if (gagRail != null && (rail.pos.equals(gagRail.getParent()) || gagRail.getReplaced() != null)) {
					if (rail.world.internal.isBlockIndirectlyGettingPowered(gagPos.internal) > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
