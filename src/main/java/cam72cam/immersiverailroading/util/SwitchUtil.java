package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.BuilderSwitch;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
			BuilderSwitch switchBuilder = (BuilderSwitch)parent.info.getBuilder();
			
			if (!switchBuilder.isOnStraight(position)) {
				return SwitchState.TURN;
			}
		}

		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition;
		if(rail.info.placementInfo.rotationQuarter % 2 == 1) { // 1 and 3 need an offset to work
			EnumFacing NormalizedFacing = rail.info.placementInfo.facing;

			if(rail.info.placementInfo.direction == TrackDirection.RIGHT) {
				NormalizedFacing = NormalizedFacing.rotateY();
			}

			if(NormalizedFacing == EnumFacing.WEST || NormalizedFacing == EnumFacing.NORTH) redstoneOrigin = redstoneOrigin.addVector(-1,0,0);
			else redstoneOrigin = redstoneOrigin.addVector(1,0,0);
		}

		for (EnumFacing facing : EnumFacing.HORIZONTALS) {
			if (rail.getWorld().isBlockIndirectlyGettingPowered(new BlockPos(redstoneOrigin).offset(facing, MathHelper.ceil(rail.info.settings.gauge.scale()))) > 0) {
				return SwitchState.TURN;
			}
		}
		return SwitchState.STRAIGHT;
	}
}
