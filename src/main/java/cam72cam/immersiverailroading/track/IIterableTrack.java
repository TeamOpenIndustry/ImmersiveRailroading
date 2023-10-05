package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.List;

public interface IIterableTrack {
    List<PosStep> getPath(double stepSize);

    List<BuilderBase> getSubBuilders();

    default double offsetFromTrack(RailInfo info, Vec3i pos, Vec3d position) {

        // Convert to relative
        Vec3d relative = position.subtract(info.placementInfo.placementPosition).subtract(pos);
        relative = relative.add(0, -(relative.y % 1), 0);

        List<PosStep> positions = getPath(info.settings.gauge.scale() / 8);

        /*
        double distSquared = 100 * 100;
        for (Vec3d gagPos : positions) {
            double offSquared = gagPos.distanceToSquared(relative);
            if (offSquared < distSquared) {
                distSquared = offSquared;
            }
        }
        return Math.sqrt(distSquared);
        */

        int left = 0;
        double leftDistance = positions.get(left).distanceToSquared(relative);
        int right = positions.size() - 1;
        double rightDistance = positions.get(right).distanceToSquared(relative);
        while (right - left > 1) {
            double midIdx = left + (right - left) / 2f;

            if (leftDistance > rightDistance) {
                left = (int) Math.floor(midIdx);
                leftDistance = positions.get(left).distanceToSquared(relative);
            } else {
                right = (int) Math.ceil(midIdx);
                rightDistance = positions.get(right).distanceToSquared(relative);
            }
        }
        return Math.sqrt(Math.min(rightDistance, leftDistance));
    }
}
