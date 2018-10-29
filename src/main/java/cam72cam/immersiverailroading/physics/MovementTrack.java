package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.library.Gauge;
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
	
	public static ITrack findTrack(World world, Vec3d currentPosition, float trainYaw, double gauge) {
		Vec3d[] positions = new Vec3d[] {
				currentPosition,
				currentPosition.add(VecUtil.fromYaw(1, trainYaw)),
				currentPosition.add(VecUtil.fromYaw(-1, trainYaw)),
		};
		
		double[] heightSkew = new double[] {
			0,
			0.25,
			-0.25,
			0.5,
			-0.5,
		};
		
		for (Vec3d pos : positions) {
			for (double height : heightSkew) {
				ITrack te = Util.getTileEntity(world, pos.addVector(0, height + 0.35, 0), true);
				if (te != null && Gauge.from(te.getTrackGauge()) == Gauge.from(gauge)) {
					return te;
				}
			}
		}
		return null;
	}
	

	public static Vec3d nextPosition(World world, Vec3d currentPosition, TileRail rail, float trainYaw, double distanceMeters) {
		double maxDelta = 0.5;
		if (distanceMeters > maxDelta) {
			double dist = 0;
			while (dist < distanceMeters - maxDelta) {
				dist += maxDelta;
				ITrack te = findTrack(world, currentPosition, trainYaw, rail.getTrackGauge());
				if (te == null) {
					return currentPosition;
				}
				Vec3d pastPos = currentPosition;
				currentPosition = te.getNextPosition(currentPosition, VecUtil.fromYaw(maxDelta, trainYaw));
				trainYaw = VecUtil.toYaw(pastPos.subtractReverse(currentPosition));
			}

			ITrack te = findTrack(world, currentPosition, trainYaw, rail.getTrackGauge());
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
		double heightOffset = 0.35 * rail.getGauge().scale();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = rail.getCenter().subtractReverse(currentPosition);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = MathHelper.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() - 1;
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
			Vec3d newpos = rail.getCenter().addVector(Math.sin(posRelYaw + yawDelt) * radius, heightOffset, -Math.cos(posRelYaw + yawDelt) * radius);
			Vec3d newneg = rail.getCenter().addVector(Math.sin(posRelYaw - yawDelt) * radius, heightOffset, -Math.cos(posRelYaw - yawDelt) * radius);

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
		} else if (rail.getType() == TrackItems.TURNTABLE) {
			double tablePos = rail.getParentTile().getTablePos();
			
			currentPosition = currentPosition.add(delta);
			
			Vec3d center = new Vec3d(rail.getParentTile().getPos()).addVector(0.5, 1 + heightOffset, 0.5);
			
			double fromCenter = currentPosition.distanceTo(center);
			
			float angle = 360/16.0f * (float)tablePos;
			
			Vec3d forward = center.add(VecUtil.fromYaw(fromCenter, angle));
			Vec3d backward = center.add(VecUtil.fromYaw(fromCenter, angle + 180));
			
			if (forward.distanceTo(currentPosition) < backward.distanceTo(currentPosition)) {
				return forward;
			} else {
				return backward;
			}
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
			
			// Reset to previous height;
			currentPosition = currentPosition.addVector(0, -heightOffset, 0);
			
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
				outPosition = new Vec3d(outPosition.x, directRail.getPos().getY() + directRail.getRailHeight(), outPosition.z);
				if (rail.getType() == TrackItems.SLOPE) {
					Vec3d offset = outPosition.subtract(currentPosition).normalize();
					float prevHeight = directRail.getPos().getY() + directRail.getRailHeight();
					float nextHeight = directRail.getPos().getY() + directRail.getRailHeight();
					float prevDist = 0;
					float nextDist = 0;
					
					TileRailBase prev = directRailFromPosition(world, outPosition.subtract(offset));
					if (prev == null) {
						prev = directRailFromPosition(world, outPosition.subtract(offset).addVector(0, 1, 0));
					}
					if (prev == null) {
						prev = directRailFromPosition(world, outPosition.subtract(offset).addVector(0, -1, 0));
					}
					if (prev != null) {
						prevHeight = prev.getPos().getY() + prev.getRailHeight();
						prevDist = (float) new Vec3d(prev.getPos()).addVector(0.5, 0, 0.5).distanceTo(outPosition); 
					}
					TileRailBase next = directRailFromPosition(world, outPosition.add(offset));
					if (next == null ) {
						next = directRailFromPosition(world, outPosition.add(offset).addVector(0, 1, 0));
					}
					if (next == null ) {
						next = directRailFromPosition(world, outPosition.add(offset).addVector(0, -1, 0));
					}
					if (next != null) {
						nextHeight = next.getPos().getY() + next.getRailHeight();
						nextDist = (float) new Vec3d(next.getPos()).addVector(0.5, 0, 0.5).distanceTo(outPosition);
					}
					
					if (prevDist + nextDist != 0) {
						float height = (prevHeight * nextDist + nextHeight * prevDist) / (nextDist + prevDist); 
						
						outPosition = new Vec3d(outPosition.x, height, outPosition.z);
					}
				}
			} else {
				outPosition = new Vec3d(outPosition.x, currentPosition.y, outPosition.z);
			}

			
			return outPosition.addVector(0, heightOffset, 0);
		}
	}
}
