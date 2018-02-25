package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import trackapi.lib.ITrack;
import trackapi.lib.Util;

public class MovementTrack {

	private static TileRailBase directRailFromPosition(World world, Vec3d position) {
		return TileRailBase.get(world, new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
	}
	

	public static Vec3d nextPosition(World world, Vec3d currentPosition, TileRail rail, float trainYaw, double distanceMeters) {
		double maxDelta = 0.5;
		if (distanceMeters > maxDelta) {
			double dist = 0;
			while (dist < distanceMeters - maxDelta) {
				dist += maxDelta;
				ITrack te = Util.getTileEntity(world, currentPosition, true);
				if (te == null) {
					return currentPosition;
				}
				Vec3d pastPos = currentPosition;
				currentPosition = te.getNextPosition(currentPosition, VecUtil.fromYaw(maxDelta, trainYaw));
				trainYaw = VecUtil.toYaw(pastPos.subtractReverse(currentPosition));
			}

			ITrack te = Util.getTileEntity(world, currentPosition, true);
			if (te == null) {
				return currentPosition;
			}
			return te.getNextPosition(currentPosition, VecUtil.fromYaw(distanceMeters % maxDelta, trainYaw));
		} else {
			return nextPositionInner(world, currentPosition, rail, trainYaw, distanceMeters);
		}
	}

	public static Vec3d nextPositionInner(World world, Vec3d currentPosition, TileRail rail, float trainYaw, double distanceMeters) {
		Vec3d delta = VecUtil.fromYaw(distanceMeters, trainYaw);
		
		if (rail == null) {
			if (!world.isRemote) {
				return null; // OFF TRACK
			} else {
				return currentPosition.add(delta);
			}
		}

		double distance = delta.lengthVector();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = rail.getCenter().subtractReverse(currentPosition);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = MathHelper.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() - 0.5;
			// Calculate the angle delta in rad (radians are awesome)
			double yawDelt = distance / radius;

			// Calculate the original next position (won't be accurate, but we
			// use it as an estimate)
			Vec3d nextPos = currentPosition.add(delta);

//			for (int i = 0; i < 90; i++) {
//				Vec3d check = rail.getCenter().addVector(Math.sin(posRelYaw + Math.toRadians(i)) * radius, 0,
//						-Math.cos(posRelYaw + Math.toRadians(i)) * radius);
//				world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, check.x, check.y, check.z, 0, 0, 0);
//			}

			// Calculate the two possible next positions (forward on the curve
			// or backward on the curve)
			Vec3d newpos = rail.getCenter().addVector(Math.sin(posRelYaw + yawDelt) * radius, 0, -Math.cos(posRelYaw + yawDelt) * radius);
			Vec3d newneg = rail.getCenter().addVector(Math.sin(posRelYaw - yawDelt) * radius, 0, -Math.cos(posRelYaw - yawDelt) * radius);

			// Return whichever position is closest to the estimated next
			// position
			if (newpos.subtract(nextPos).lengthVector() < newneg.subtract(nextPos).lengthVector()) {
				return newpos;
			} else {
				return newneg;
			}
		} else if (rail.getType() == TrackItems.CROSSING) {
			delta = VecUtil.fromYaw(distance, EnumFacing.fromAngle(trainYaw).getHorizontalAngle());
			return currentPosition.add(delta);
		} else {
			// delta should be in the direction of rotationYaw instead of front or rear
			// since large changes can occur if the train is way off center
			delta = VecUtil.fromYaw(distance, trainYaw);
			/*
			// Check that we are not moving in the wrong axis along a track
			if (EnumFacing.fromAngle(trainYaw).getAxis() != rail.getFacing().getAxis() && rail.getType() != TrackItems.CROSSING) {
				if (!world.isRemote) {
					System.out.println("Wrong track direction");
					startPos.setDead();
					return position;
				}
			}*/
			float angle = rail.getRotationQuarter()/4f * 90 + rail.getFacing().getHorizontalAngle();
			
			// |>----O-----|
			// |--->-O-----|
			// |-----O->---|
			// |-----O---->|
			// |<----O-----|
			// |---<-O-----|
			// |-----O-<---|
			// |-----O----<|
			
			
			Vec3d center = rail.getPlacementPosition();
			
			double toCenter = center.distanceTo(currentPosition);
			
			Vec3d possiblePositive = center.add(VecUtil.fromYaw(toCenter, angle));
			Vec3d possibleNegative = center.add(VecUtil.fromYaw(-toCenter, angle));
			
			double angularDistance = 0; 
			if (possiblePositive.distanceTo(currentPosition) < possibleNegative.distanceTo(currentPosition)) {
				angularDistance = toCenter;
			} else {
				angularDistance = -toCenter;
			}
			
			possiblePositive = center.add(VecUtil.fromYaw(angularDistance + distance, angle));
			possibleNegative = center.add(VecUtil.fromYaw(angularDistance - distance, angle));
			
			Vec3d outPosition;
			if (possiblePositive.distanceTo(currentPosition.add(delta)) < possibleNegative.distanceTo(currentPosition.add(delta))) {
				outPosition = possiblePositive;
			} else {
				outPosition = possibleNegative;
			}

			// Update y position
			TileRailBase directRail = directRailFromPosition(world, outPosition);
			if (directRail != null) {
				outPosition = new Vec3d(outPosition.x, directRail.getPos().getY() + directRail.getHeight(), outPosition.z);
			} else {
				outPosition = new Vec3d(outPosition.x, currentPosition.y, outPosition.z);
			}
			
			return outPosition;
		}
	}
}
