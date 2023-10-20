package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.List;

public interface IIterableTrack {
    List<PosStep> getPath(double stepSize);

    List<BuilderBase> getSubBuilders();

    default double offsetFromTrack(RailInfo info, Vec3i pos, Vec3d position) {
        double distSquared = 100 * 100;

        // Convert to relative
        position = position.subtract(info.placementInfo.placementPosition).subtract(pos);

        for (Vec3d gagPos : getPath(info.settings.gauge.scale()/8)) {
            double offSquared = gagPos.distanceToSquared(position.add(0, -(position.y % 1), 0));
            if (offSquared < distSquared) {
                distSquared = offSquared;
            }
        }
        return Math.sqrt(distSquared);
    }
}
