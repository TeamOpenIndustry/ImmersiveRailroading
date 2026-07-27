package cam72cam.immersiverailroading.track;

import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public record PosRollOffset(Vec3d point, double roll, double yOffset, double zOffset) {

    public static List<Vec3d> getPoints(List<PosRollOffset> posRollOffset) {
        List<Vec3d> points = new ArrayList<>();
        for (PosRollOffset rollOffset : posRollOffset) points.add(rollOffset.point);
        return points;
    }

    public static List<Double> getRolls(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for (PosRollOffset rollOffset : posRollOffset) points.add(rollOffset.roll);
        return points;
    }

    public static List<Double> getYOffsets(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for (PosRollOffset rollOffset : posRollOffset) points.add(rollOffset.yOffset);
        return points;
    }

    public static List<Double> getZOffsets(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for (PosRollOffset rollOffset : posRollOffset) points.add(rollOffset.zOffset);
        return points;
    }

}
