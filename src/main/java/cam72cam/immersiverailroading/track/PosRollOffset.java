package cam72cam.immersiverailroading.track;

import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class PosRollOffset {
    public final Vec3d point;
    public final double roll;
    public final double yOffset;
    public final double zOffset;
    public PosRollOffset(Vec3d point, double roll, double yOffset, double zOffset) {
        this.point = point;
        this.roll = roll;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    public static List<Vec3d> getPoints(List<PosRollOffset> posRollOffset) {
        List<Vec3d> points = new ArrayList<>();
        for(int i = 0; i < posRollOffset.size(); i++)points.add(posRollOffset.get(i).point);
        return points;
    }

    public static List<Double> getRolls(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for(int i = 0; i < posRollOffset.size(); i++)points.add(posRollOffset.get(i).roll);
        return points;
    }

    public static List<Double> getYOffsets(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for(int i = 0; i < posRollOffset.size(); i++)points.add(posRollOffset.get(i).yOffset);
        return points;
    }

    public static List<Double> getZOffsets(List<PosRollOffset> posRollOffset) {
        List<Double> points = new ArrayList<>();
        for(int i = 0; i < posRollOffset.size(); i++)points.add(posRollOffset.get(i).zOffset);
        return points;
    }
}
