package cam72cam.immersiverailroading.data;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.util.SwitchUtil;
import cam72cam.mod.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackMultiPath implements ITrack {
    // TODO set?
    private final List<TrackInfo> paths;
    private final WorldData data;

    public TrackMultiPath(WorldData data, List<TrackInfo> infos) {
        this.data = data;
        this.paths = infos;
    }

    @Override
    public double getTrackGauge() {
        // TODO filter multi path based on desired gauge when needed
        return paths.get(0).settings.gauge.value();
    }

    @Override
    public Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion) {
        /*double distanceMetersSq = motion.lengthSquared();
        double maxDistance = 0.25;
        if (distanceMetersSq*0.9 > maxDistance * maxDistance) {
            // 0.9 forces at least one iteration + scaling
            return iterativePathing(currentPosition,  motion, maxDistance);
        }*/
        return getNextPositionShort(currentPosition, motion);
    }

    /*private Vec3d iterativePathing(Vec3d currentPosition, Vec3d motion, double maxDistance) {
        Vec3d startPos = currentPosition;
        Vec3d prevPosition = currentPosition;
        double totalDistance = motion.length();
        double maxDistanceSquared = maxDistance * maxDistance;
        double motionLengthSquared = motion.lengthSquared();

        Vec3i teBlockPosition = new Vec3i(currentPosition);

        for (double currentDistance = 0; currentDistance < totalDistance; currentDistance += maxDistance) {
            Vec3i currentBlockPosition = new Vec3i(currentPosition);
            if (!currentBlockPosition.equals(teBlockPosition)) {
                teBlockPosition = currentBlockPosition;

                te = MovementTrack.findTrack(world, currentPosition, VecUtil.toWrongYaw(motion), gauge);
                if (te == null) {
                    // Stuck
                    return currentPosition;
                }
            }

            // Correct motion length (if off by 5%)
            if (motionLengthSquared > maxDistanceSquared || motionLengthSquared < maxDistanceSquared * 0.95) {
                motion = motion.scale(maxDistance / Math.sqrt(motionLengthSquared));
            }

            prevPosition = currentPosition;
            currentPosition = te instanceof TileRailBase ? ((TileRailBase) te).getNextPositionShort(currentPosition, motion) : te.getNextPosition(currentPosition, motion);
            motion = currentPosition.subtract(prevPosition);
            motionLengthSquared = motion.lengthSquared();

            if (motionLengthSquared == 0) {
                // Stuck
                return prevPosition;
            }
        }

        // prevPosition + motion scaled to remaining distance
        //double scale = (totalDistance - startPos.distanceTo(prevPosition)) / motion.length();
        //if (scale > 0.00001 && scale < 1.5) {
        //	currentPosition = prevPosition.add(motion.scale(scale));
        //}
        currentPosition = startPos.add(currentPosition.subtract(startPos).normalize().scale(totalDistance));

        return currentPosition;
    }*/

    private Vec3d getNextPositionShort(Vec3d currentPosition, Vec3d motion) {
        if (paths.size() == 1) {
            // Optimized version of the below looping when no overlapping occurs
            return new TrackPath(paths.get(0)).getNextPosition(currentPosition, motion);
        }
        // Complex case with overlapping segments

        // For switch turns, it's either
        // Block -> Info -> Parent (switch)
        // Or
        // Block -> Info -> Parent(segments) -> Parent (switch)
        // In both scenarios the root parent is the same and both legs exist within the data

        // This is not efficient!

        Map<TrackInfo, TrackInfo> parents = new HashMap<>();

        for (TrackInfo info : paths) {
            parents.put(info, data.getTopParent(info));
        }

        Vec3d nextPos = currentPosition;
        Vec3d predictedPos = currentPosition.add(motion);

        for (TrackInfo info : paths) {
            Vec3d potential;

            TrackInfo parent = parents.get(info);
            if (parent == null) {
                continue;
            }
            if (parent.settings.type == TrackItems.SWITCH) {
                TrackInfo straight = null;
                TrackInfo turn = null;
                for (Map.Entry<TrackInfo, TrackInfo> entry : parents.entrySet()) {
                    if (entry.getValue() == parent) {
                        if (entry.getKey().settings.type == TrackItems.SWITCH) {
                            straight = entry.getKey();
                        } else {
                            // TODO actual validation of type (TURN/CUSTOM)
                            turn = entry.getKey();
                        }
                    }
                }
                if (turn != null && straight != null) {
                    parents.remove(turn);
                    parents.remove(straight);
                    info = SwitchUtil.getSwitchLeg(straight, turn, currentPosition);
                }
            }
            potential = new TrackPath(info).getNextPosition(currentPosition, motion);

            if (nextPos == currentPosition || potential.distanceToSquared(predictedPos) < nextPos.distanceToSquared(predictedPos)) {
                nextPos = potential;
            }
        }

        return nextPos;
    }
}
