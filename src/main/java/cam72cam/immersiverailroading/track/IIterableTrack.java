package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public interface IIterableTrack {
    List<PosStep> getPath(double stepSize);

    List<BuilderBase> getSubBuilders();

    default boolean isOnTrack(RailInfo info, Vec3d position) {
        for (Vec3d gagPos : getPath(info.settings.gauge.scale()/8)) {
            gagPos = gagPos.add(info.placementInfo.placementPosition);
            if (gagPos.distanceTo(position.addVector(0, -(position.y % 1), 0)) < info.settings.gauge.scale()/2) {
                return true;
            }
        }
        return false;
    }
}
