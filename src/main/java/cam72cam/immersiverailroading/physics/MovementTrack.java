package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.thirdparty.trackapi.IRPathingData;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.world.World;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;

import java.util.List;

public class MovementTrack {

	public static ITrack findTrack(World world, Vec3d currentPosition, float trainYaw, double gauge) {
		Vec3d[] positions = new Vec3d[] {
				currentPosition,
				currentPosition.add(VecUtil.fromWrongYaw(1, trainYaw)),
				currentPosition.add(VecUtil.fromWrongYaw(-1, trainYaw)),
		};
		
		double[] heightSkew = new double[] {
			0,
			0.25,
			-0.25,
			0.5,
			-0.5,
			0.75,
			-0.75
		};
		
		for (Vec3d pos : positions) {
			for (double height : heightSkew) {
				ITrack te = ITrack.get(world, pos.add(0, height + (currentPosition.y%1), 0), true);
				if (te != null && Gauge.from(te.getTrackGauges()[0]) == Gauge.from(gauge)) {
					return te;
				}
				// HACK for cross gauge
				TileRailBase rail = world.getBlockEntity(new Vec3i(pos).add(new Vec3i(0, (int)(height + (currentPosition.y%1)), 0)), TileRailBase.class);
				if (rail != null && rail.getParentReplaced() != null) {
					return rail;
				}
			}
		}
		return null;
	}

	public static void iterativePathing(World world, IRPathingData startPosition, ITrack te, double gauge, Vec3d motion, double maxDistance) {
		IRPathingData iterationPosition = startPosition.clone();
		IRPathingData prevPosition;
		double totalDistance = motion.length();
		double maxDistanceSquared = maxDistance * maxDistance;
		double motionLengthSquared = motion.lengthSquared();

		Vec3i teBlockPosition = new Vec3i(startPosition.getUMCPos());

		for (double iteratedDistance = 0; iteratedDistance < totalDistance; iteratedDistance += maxDistance) {
			Vec3i iterationBlockPosition = new Vec3i(iterationPosition.getUMCPos());
			if (!iterationBlockPosition.equals(teBlockPosition)) {
				teBlockPosition = iterationBlockPosition;

				te = findTrack(world, iterationPosition.getUMCPos(), VecUtil.toWrongYaw(motion), gauge);
				if (te == null) {
					// Stuck
					startPosition.advanceTo(iterationPosition);// It's sticking... seems not need to change roll
					return;
				}
			}

			// Correct motion length (if off by 5%)
			if (motionLengthSquared > maxDistanceSquared || motionLengthSquared < maxDistanceSquared * 0.95) {
				motion = motion.scale(maxDistance / Math.sqrt(motionLengthSquared));
			}

			prevPosition = iterationPosition.clone();

            if (te instanceof TileRailBase base) {
                base.getNextPositionShort(iterationPosition, motion, gauge);
            } else {
                te.getNextPosition(iterationPosition, motion, gauge);
            }

            motion = iterationPosition.getUMCPos().subtract(prevPosition.getUMCPos());
			motionLengthSquared = motion.lengthSquared();

			if (motionLengthSquared == 0) {
				// Stuck
				startPosition.advanceTo(prevPosition);
				return;
			}
		}

		// prevPosition + motion scaled to remaining distance
		//double scale = (totalDistance - startPos.distanceTo(prevPosition)) / motion.length();
		//if (scale > 0.00001 && scale < 1.5) {
		//	iterationPosition = prevPosition.add(motion.scale(scale));
		//}
		Vec3d iterationPosRescaled = startPosition.getUMCPos().add(iterationPosition.getUMCPos().subtract(startPosition.getUMCPos()).normalize().scale(totalDistance));

		startPosition.advanceTo(iterationPosRescaled, iterationPosition.getRoll());//we don't need to rescale the roll!
	}

	public static void nextPositionDirect(World world, IRPathingData currentPosition, TileRail rail, Vec3d delta, double gauge) {//todo: gauges
		if (rail == null) {
			if (world.isServer) {
				return; // OFF TRACK
			} else {
				currentPosition.advanceTo(currentPosition.getUMCPos().add(delta), currentPosition.getRoll());
				return;
			}
		}
		int invertRollMultiplier = 1;

		double railHeight = rail.info.getTrackHeight();
		double distance = delta.length();
		double heightOffset = railHeight * rail.info.settings.gauge.scale();

		if (rail.info.settings.type == TrackItems.CROSSING) {
			delta = VecUtil.fromWrongYaw(distance, Facing.fromAngle(VecUtil.toWrongYaw(delta)).getAngle());
			currentPosition.advanceTo(currentPosition.getUMCPos().add(delta), currentPosition.getRoll());
			return;
		} else if (rail.info.settings.type.isTable()) {
			double tablePos = rail.getParentTile().info.tablePos;

			currentPosition.advanceTo(currentPosition.getUMCPos().add(delta), currentPosition.getRoll());

			Facing placementFacing = rail.info.placementInfo.facing();
			Vec3d center, forward, backward;
			double distanceToCenter;
			float angle;
			if (rail.info.settings.type == TrackItems.TURNTABLE) {
				angle = (float) tablePos + placementFacing.getAngle();
				center = new Vec3d(rail.getParentTile().getPos()).add(0.5, 1 + heightOffset, 0.5);
				distanceToCenter = currentPosition.getUMCPos().distanceTo(center);
			} else {
				//Must be transfer table
				int halfGauge = (int) Math.floor((rail.info.settings.gauge.value() * 1.1 + 0.5) / 2);
				int width = rail.info.settings.transfertableEntrySpacing * (rail.info.settings.transfertableEntryCount - 1) + halfGauge + 2;
				Vec3i mainOffset = new Vec3i(-width / 2, 1, rail.info.settings.length / 2);
				center = new Vec3d(rail.getPos().subtract(mainOffset.rotate(Rotation.from(placementFacing))));
				double xValue;
				switch (placementFacing) {
					case SOUTH:
						xValue = -tablePos - rail.info.placementInfo.placementPosition.x % 1 - 1;
						break;
					case NORTH:
						xValue = -tablePos + rail.info.placementInfo.placementPosition.x % 1;
						break;
					case EAST:
						xValue = -tablePos + rail.info.placementInfo.placementPosition.z % 1;
						break;
					case WEST:
						xValue = -tablePos - rail.info.placementInfo.placementPosition.z % 1 - 1;
						break;
					default:
						//WTH
						return;
				}
				angle = -placementFacing.getAngle() + 180;
				center = center.add(
						new Vec3d(xValue, 2 + heightOffset, rail.info.settings.length / 2d).rotateYaw(angle));
				distanceToCenter = currentPosition.getUMCPos().distanceTo(center);
			}

			forward = center.add(VecUtil.fromWrongYaw(distanceToCenter, angle));
			backward = center.add(VecUtil.fromWrongYaw(distanceToCenter, angle + 180));

			if (forward.distanceToSquared(currentPosition.getUMCPos()) < backward.distanceToSquared(currentPosition.getUMCPos())) {
				currentPosition.advanceTo(forward, currentPosition.getRoll());
				return;
			} else {
				currentPosition.advanceTo(backward, currentPosition.getRoll());
				return;
			}
		} else if (rail.info.getBuilder(world) instanceof IIterableTrack) {
			/*
			 * Discovery: This is not accurate for distances less than 0.1m
			 * Since we snap to the line, small distances are snapped to a tangent that's further than they are
			 * trying to move.  Instead we should probably calculate the vector between the closest pos
			 * and the current pos and move distance along that.  How would that work for slopes at the ends? just fine?
			 */
			List<VecYPR> positions = ((IIterableTrack) rail.info.getBuilder(world)).getPath(0.25 * rail.info.settings.gauge.scale());
			Vec3d center = rail.info.placementInfo.placementPosition.add(rail.getPos()).add(0, heightOffset, 0);
			Vec3d target = currentPosition.getUMCPos().add(delta);
			Vec3d relative = target.subtract(center);

			if (positions.isEmpty()) {
				ImmersiveRailroading.error("Invalid track path %s", rail.info.uniqueID);

				currentPosition.advanceTo(currentPosition.getUMCPos().add(delta), currentPosition.getRoll());
				return ; // keep in same place for debugging
			}
			if (positions.size() == 1) {
				// track with length == 1
				VecYPR pos = positions.getFirst();
				Vec3d offset = VecUtil.fromYaw(delta.length(), pos.getYaw());
				Vec3d result = currentPosition.getUMCPos().add(offset);
				Vec3d resultOpposite = currentPosition.getUMCPos().subtract(offset);

				double roll = pos.getRoll();

				if (result.distanceToSquared(target) < resultOpposite.distanceToSquared(target)) {
					if(VecUtil.delta(
							VecUtil.toWrongYaw(pos.getYaw()),
							VecUtil.toWrongYaw(result.subtract(currentPosition.getUMCPos()))
					) > 90) invertRollMultiplier = -1;

					currentPosition.advanceTo(result, roll * invertRollMultiplier);
                } else {
					if(VecUtil.delta(
							VecUtil.toWrongYaw(pos.getYaw()),
							VecUtil.toWrongYaw(resultOpposite.subtract(currentPosition.getUMCPos()))
					) > 90) invertRollMultiplier = -1;

					currentPosition.advanceTo(resultOpposite, roll * invertRollMultiplier);
                }
                return;
            }

			/* Simple ordered binary search ***
			l    c      r
			l    c  r
			    lc  r
			    lcr
			    lr

			 *** Updated to step by 1/3rds since this is not a purely linear search.
			 */
			int left = 0;
			double leftDistance = positions.get(left).distanceToSquared(relative);
			int right = positions.size() - 1;
			double rightDistance = positions.get(right).distanceToSquared(relative);
			while (right - left > 1) {
				if (leftDistance > rightDistance) {
					left = (int) Math.ceil(left + (right - left) / 3f);
					leftDistance = positions.get(left).distanceToSquared(relative);
				} else {
					right = (int) Math.floor(right + (left - right) / 3f);
					rightDistance = positions.get(right).distanceToSquared(relative);
				}
			}
			if (right == left) {
				ImmersiveRailroading.warn("Correcting track pathing tree...");
				// Hack for edge case
				if (right == positions.size() -1) {
					left -= 1;
				} else {
					right += 1;
				}
			}

			VecYPR leftPos = positions.get(left);
			VecYPR rightPos = positions.get(right);

			if (leftDistance < 0.000001) {
				if(VecUtil.delta(
						VecUtil.toWrongYaw(leftPos.getYaw()),
						VecUtil.toWrongYaw(center.add(leftPos).subtract(currentPosition.getUMCPos()))
				) > 90) invertRollMultiplier = -1;

				currentPosition.advanceTo(center.add(leftPos), leftPos.getRoll() * invertRollMultiplier);
				return ;
			}
			if (rightDistance < 0.000001) {
				if(VecUtil.delta(VecUtil.toWrongYaw(rightPos.getYaw()),
						VecUtil.toWrongYaw(center.add(rightPos).subtract(currentPosition.getUMCPos()))
				) > 90) invertRollMultiplier = -1;

				currentPosition.advanceTo(center.add(rightPos), rightPos.getRoll() * invertRollMultiplier);
				return ;
			}

			Vec3d between = rightPos.subtract(leftPos);
			double t = Math.sqrt(leftDistance) / between.length();
			Vec3d offset = between.scale(t);
			// Weird edge case where we need to move in the opposite direction since we are given a position past the end of pathing
			Vec3d point = center.add(leftPos);
			Vec3d result = point.add(offset);
			Vec3d resultOpposite = point.subtract(offset);
			double resRoll = leftPos.getRoll() + (rightPos.getRoll() - leftPos.getRoll()) * t;
			double resRollOpposite = leftPos.getRoll() - (rightPos.getRoll() - leftPos.getRoll()) * t;

			if (result.distanceToSquared(target) < resultOpposite.distanceToSquared(target)) {
				if(VecUtil.delta(
						VecUtil.toWrongYaw(leftPos.getYaw()),
						VecUtil.toWrongYaw(result.subtract(currentPosition.getUMCPos()))
				) > 90) invertRollMultiplier = -1;

				currentPosition.advanceTo(result, resRoll * invertRollMultiplier);
				return;
			} else {
				if(VecUtil.delta(
						VecUtil.toWrongYaw(leftPos.getYaw()),
						VecUtil.toWrongYaw(resultOpposite.subtract(currentPosition.getUMCPos()))
				) > 90) invertRollMultiplier = -1;

				currentPosition.advanceTo(resultOpposite, resRollOpposite * invertRollMultiplier);
				return;
			}
		}
		currentPosition.advanceTo(currentPosition.getUMCPos().add(delta), currentPosition.getRoll());
	}
}
