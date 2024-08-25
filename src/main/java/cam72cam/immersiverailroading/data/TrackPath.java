package cam72cam.immersiverailroading.data;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.track.PosStep;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.Facing;

import java.util.List;

public class TrackPath implements ITrack {
    final TrackInfo info;

    public TrackPath(TrackInfo info) {
        this.info = info;
    }

    @Override
    public double getTrackGauge() {
        return info.settings.gauge.value();
    }

    @Override
    public Vec3d getNextPosition(Vec3d currentPosition, Vec3d delta) {
        double railHeight = this.info.getTrackHeight();
        double distance = delta.length();
        double heightOffset = railHeight * this.info.settings.gauge.scale();

        if (this.info.settings.type == TrackItems.CROSSING) {
            delta = VecUtil.fromWrongYaw(distance, Facing.fromAngle(VecUtil.toWrongYaw(delta)).getAngle());
            return currentPosition.add(delta);
        } else if (this.info.settings.type == TrackItems.TURNTABLE) {
            double tablePos = this.info.tablePos;

            currentPosition = currentPosition.add(delta);

            // TODO broken rail.getParentTile().getPos()
            Vec3d center = this.info.placementInfo.placementPosition.add(0.5, 1 + heightOffset, 0.5);

            double fromCenter = currentPosition.distanceTo(center);

            float angle = (float)tablePos + this.info.placementInfo.facing().getAngle();

            Vec3d forward = center.add(VecUtil.fromWrongYaw(fromCenter, angle));
            Vec3d backward = center.add(VecUtil.fromWrongYaw(fromCenter, angle + 180));

            if (forward.distanceToSquared(currentPosition) < backward.distanceToSquared(currentPosition)) {
                return forward;
            } else {
                return backward;
            }
        } else if (this.info.getBuilder(null) instanceof IIterableTrack) {
            /*
             * Discovery: This is not accurate for distances less than 0.1m
             * Since we snap to the line, small distances are snapped to a tangent that's further than they are
             * trying to move.  Instead we should probably calculate the vector between the closest pos
             * and the current pos and move distance along that.  How would that work for slopes at the ends? just fine?
             */
            List<PosStep> positions = ((IIterableTrack) this.info.getBuilder(null)).getPath(this.info.settings.gauge.scale());
            Vec3d center = this.info.placementInfo.placementPosition/*.add(rail.getPos())*/.add(0, heightOffset, 0);
            Vec3d target = currentPosition.add(delta);
            Vec3d relative = target.subtract(center);

            if (positions.isEmpty()) {
                ImmersiveRailroading.error("Invalid track path %s", this.info.uniqueID);
                return currentPosition; // keep in same place for debugging
            }
            if (positions.size() == 1) {
                // track with length == 1
                PosStep pos = positions.get(0);
                Vec3d offset = VecUtil.fromYaw(delta.length(), pos.yaw);
                Vec3d result = currentPosition.add(offset);
                Vec3d resultOpposite = currentPosition.subtract(offset);
                if (result.distanceToSquared(target) < resultOpposite.distanceToSquared(target)) {
                    return result;
                } else {
                    return resultOpposite;
                }
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

            PosStep leftPos = positions.get(left);
            PosStep rightPos = positions.get(right);

            if (leftDistance < 0.000001) {
                return center.add(leftPos);
            }
            if (rightDistance < 0.000001) {
                return center.add(rightPos);
            }

            Vec3d between = rightPos.subtract(leftPos);
            Vec3d offset = between.scale(Math.sqrt(leftDistance) / between.length());
            // Weird edge case where we need to move in the opposite direction since we are given a position past the end of pathing
            Vec3d point = center.add(leftPos);
            Vec3d result = point.add(offset);
            Vec3d resultOpposite = point.subtract(offset);
            if (result.distanceToSquared(target) < resultOpposite.distanceToSquared(target)) {
                return result;
            } else {
                return resultOpposite;
            }
        }
        return currentPosition.add(delta);
    }
}
