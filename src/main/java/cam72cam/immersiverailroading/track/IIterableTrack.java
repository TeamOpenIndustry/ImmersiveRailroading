package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.ImmersiveRailroading;
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

        List<PosStep> positions = getPath(info.settings.gauge.scale());

        /*double distSquared = 100 * 100;
        for (Vec3d gagPos : positions) {
            double offSquared = gagPos.distanceToSquared(relative);
            if (offSquared < distSquared) {
                distSquared = offSquared;
            }
        }*/

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


        double resultSquared = Math.min(rightDistance, leftDistance);

        /*if (resultSquared != distSquared) {
            ImmersiveRailroading.info("orig: %s, new %s", Math.sqrt(distSquared), Math.sqrt(resultSquared));
        }*/

        return Math.sqrt(resultSquared);
    }
}
