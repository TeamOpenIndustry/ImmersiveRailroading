package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderTurn extends BuilderIterator {
    public BuilderTurn(RailInfo info, BlockPos pos) {
        super(info, pos);
    }

    @Override
    public List<PosStep> getPath(double stepSize) {
        float radius = info.settings.length;

        float quarters = Math.max(1, info.settings.quarters)/ 4f * 90;
        float endAngle = 90 - info.placementInfo.rotationQuarter / 4f * 90;
        float startAngle = endAngle - quarters;

        if (info.placementInfo.direction == TrackDirection.RIGHT) { // actually left
            startAngle = 180 + 90 + info.placementInfo.rotationQuarter/4f * 90;
            endAngle = startAngle + quarters;
        }

        // Hack the radius
        radius -= 1;

        startAngle = (float)Math.toRadians(startAngle);
        endAngle = (float)Math.toRadians(endAngle);

        double xPos = Math.sin(endAngle) * (radius);
        double zPos = Math.cos(endAngle) * (radius);
        if (info.placementInfo.direction == TrackDirection.RIGHT) { // actually left
            xPos = Math.sin(startAngle) * (radius);
            zPos = Math.cos(startAngle) * (radius);
        }
        Vec3d center = new Vec3d(xPos, 0, zPos);

        List<PosStep> res = new ArrayList<>();
        List<PosStep> resReverse = new ArrayList<>();

        if (startAngle > endAngle) {
            float tmp = startAngle;
            startAngle = endAngle;
            endAngle = tmp;
        }

        for (float i = startAngle; i < (endAngle+startAngle)/2 + stepSize/radius; i += stepSize/radius) {
            Vec3d point = new Vec3d(Math.sin(i) * radius, 0,Math.cos(i) * radius);
            point = point.subtract(center);
            res.add(new PosStep(point, 90 - (float) Math.toDegrees(i) + 180));
        }
        for (float i = endAngle; i >= (endAngle+startAngle)/2; i -= stepSize/radius) {
            Vec3d point = new Vec3d(Math.sin(i) * radius, 0,Math.cos(i) * radius);
            point = point.subtract(center);
            resReverse.add(new PosStep(point, 90 - (float) Math.toDegrees(i)+180));
        }
        Collections.reverse(resReverse);
        res.addAll(resReverse);

        return res;
    }
}
