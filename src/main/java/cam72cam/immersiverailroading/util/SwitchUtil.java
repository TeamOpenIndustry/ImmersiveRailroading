package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class SwitchUtil {
	public static SwitchState getSwitchState(TileRail rail) {
		return getSwitchState(rail, null);
	}

	public static SwitchState getSwitchState(TileRail rail, Vec3d position) {
		if (rail == null) {
			return SwitchState.NONE;
		}
		TileRail parent = rail.getParentTile();
		if (parent == null) {
			return SwitchState.NONE;
		}
		
		if (rail.info.settings.type != TrackItems.TURN && rail.info.settings.type != TrackItems.CUSTOM) {
			return SwitchState.NONE;
		}
		if (parent.info.settings.type != TrackItems.SWITCH) {
			return SwitchState.NONE;
		}

		if (position != null) {
			IIterableTrack switchBuilder = (IIterableTrack) parent.info.getBuilder(rail.getWorld());
			IIterableTrack turnBuilder = (IIterableTrack) rail.info.getBuilder(rail.getWorld());
			double isOnStraight = switchBuilder.offsetFromTrack(parent.info, parent.getPos(), position);
			double isOnTurn = turnBuilder.offsetFromTrack(rail.info, rail.getPos(), position);

			if (Math.abs(isOnStraight - isOnTurn) > rail.info.settings.gauge.scale() / 16) {
				if (isOnStraight > isOnTurn) {
					return SwitchState.NONE;
				} else {
					return SwitchState.STRAIGHT;
				}
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
		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition.add(rail.getPos());
		double horiz = rail.info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && rail.info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		int scale = (int)Math.round(horiz);
		for (int x = -scale; x <= scale; x++) {
			for (int z = -scale; z <= scale; z++) {
				Vec3i gagPos = new Vec3i(redstoneOrigin.add(new Vec3d(x, 0, z)));
				TileRailBase gagRail = rail.getWorld().getBlockEntity(gagPos, TileRailBase.class);
				if (gagRail != null && (rail.getPos().equals(gagRail.getParent()) || gagRail.getReplaced() != null)) {
					if (rail.getWorld().getRedstone(gagPos) > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
