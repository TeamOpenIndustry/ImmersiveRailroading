package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;

public class CubicCurve {
    public final Vec3d p1;
    public final Vec3d ctrl1;
    public final Vec3d ctrl2;
    public final Vec3d p2;

    public double[] t;
    public double[] len;
    public int segment;

    //http://spencermortensen.com/articles/bezier-circle/
    public final static double c = 0.55191502449;

    public CubicCurve(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2) {
        this.p1 = p1;
        this.ctrl1 = ctrl1;
        this.ctrl2 = ctrl2;
        this.p2 = p2;
    }

    public static CubicCurve circle(int radius, float degrees) {
        float cRadScale = degrees / 90;
        Vec3d p1 = new Vec3d(0, 0, radius);
        Vec3d ctrl1 = new Vec3d(cRadScale * c * radius, 0, radius);
        Vec3d ctrl2 = new Vec3d(radius, 0, cRadScale * c * radius);
        Vec3d p2 = new Vec3d(radius, 0, 0);

        Matrix4 quart = new Matrix4();
        quart.rotate(Math.toRadians(-90+degrees), 0, 1, 0);

        return new CubicCurve(p1, ctrl1, quart.apply(ctrl2), quart.apply(p2)).apply(new Matrix4().translate(0, 0, -radius));
    }

    public CubicCurve apply(Matrix4 mat) {
        return new CubicCurve(
                mat.apply(p1),
                mat.apply(ctrl1),
                mat.apply(ctrl2),
                mat.apply(p2)
        );
    }

    public CubicCurve reverse() {
        return new CubicCurve(p2, ctrl2, ctrl1, p1);
    }

    public CubicCurve truncate(double t) {
        Vec3d midpoint = this.ctrl1.add(this.ctrl2).scale(t);
        Vec3d ctrl1 = p1.add(this.ctrl1).scale(t);
        Vec3d ctrl2 = p2.add(this.ctrl2).scale(t);

        Vec3d temp = ctrl2.add(midpoint).scale(t);
        ctrl2 = ctrl1.add(midpoint).scale(t);
        midpoint = ctrl2.add(temp).scale(t);
        return new CubicCurve(
                p1,
                ctrl1,
                ctrl2,
                midpoint
        );
    }

    public Pair<CubicCurve, CubicCurve> split(double t) {
        return Pair.of(this.truncate(t), this.reverse().truncate(1-t));
    }

    public Vec3d position(double t) {
        //Using Vec3d will cause almost 2850% performance decrease
        double u = 1 - t;

        double d1 = u * u * u;
        double d2 = 3 * u * u * t;
        double d3 = 3 * u * t * t;
        double d4 = t * t * t;

        double x = p1.x * d1 + ctrl1.x * d2 + ctrl2.x * d3 + p2.x * d4;
        double y = p1.y * d1 + ctrl1.y * d2 + ctrl2.y * d3 + p2.y * d4;
        double z = p1.z * d1 + ctrl1.z * d2 + ctrl2.z * d3 + p2.z * d4;
        return new Vec3d(x, y, z);
    }

    public Vec3d derivative(double t){
        //WILL CAUSE 1000%+ decrease if using Vec3d
        double u = 1 - t;
        double d1 = 3 * u * u;
        double d2 = 6 * u * t;
        double d3 = 3 * t * t;

        double dx = d1 * (ctrl1.x - p1.x) + d2 * (ctrl2.x - ctrl1.x) + d3 * (p2.x - ctrl2.x);
        double dy = d1 * (ctrl1.y - p1.y) + d2 * (ctrl2.y - ctrl1.y) + d3 * (p2.y - ctrl2.y);
        double dz = d1 * (ctrl1.z - p1.z) + d2 * (ctrl2.z - ctrl1.z) + d3 * (p2.z - ctrl2.z);

        return new Vec3d(dx, dy, dz);
    }

    public double lengthWithCache(int iterations){
        this.segment = iterations;
        this.t = new double[segment + 10];
        this.len = new double[segment + 10];
        double length = 0.0;
        double tStep = 1.0 / (double) iterations;
        Vec3d prevDeriv = derivative(0);
        double prevSpeed = prevDeriv.length();
        //Cache it
        t[0] = 0.0;
        len[0] = 0.0;

        for (int i = 1; i <= (double) iterations; i++) {
            double pos = i * tStep;
            Vec3d deriv = derivative(pos);
            double speed = deriv.length();

            length += (prevSpeed + speed) * tStep / 2.0;
            t[i] = pos;
            len[i] = length;
            prevSpeed = speed;
        }
        t[segment] = 1;//The final index
        return length;
    }

    public double lengthInBetween(double start, double end, double iter){
        if(start == end){
            return 0;
        }
        double length = 0.0;
        double tStep = (end - start) / iter;
        Vec3d prevDeriv = derivative(start);
        double prevSpeed = prevDeriv.length();

        for (double i = start + tStep; i <= end; i+=tStep) {
            Vec3d deriv = derivative(i);
            double speed = deriv.length();

            length += (prevSpeed + speed) * tStep / 2.0;
            prevSpeed = speed;
        }
        return length;
    }

    public List<Vec3d> toList(double stepSize) {
        List<Vec3d> result = new ArrayList<>();
        result.add(p1);
        if(p1.equals(p2)){
            return result;
        }

        double lastLength = 0;
        double error = 0.001 * stepSize;

        for (int i = 0; i < segment; i++) {
            if(len[i] - lastLength <= stepSize && len[i+1] - lastLength > stepSize){
                double low = t[i];
                double high = t[i+1];
                double currentLen = len[i];
                double mid = (low + high) / 2;

                for(int j = 1; j <= 7; j++){
                    mid = (low + high) / 2;
                    double test = lengthInBetween(low, mid, 10);
                    if(Math.abs(currentLen + test - lastLength - stepSize) < error){
                        break;
                    }

                    if(currentLen + test < lastLength + stepSize){
                        low = mid;
                        currentLen += test;
                    } else if (currentLen + test > lastLength + stepSize) {
                        high = mid;
                    } else {
                        break;
                    }
                }

                result.add(position(mid));
                lastLength = currentLen + lengthInBetween(low, mid, 10);
            }
        }

        if(len[segment] - lastLength >= 0.8 * stepSize){
            result.add(p2);
        }

        return result;
    }

    public float angleStop() {
        return VecUtil.toYaw(p2.subtract(ctrl2));
    }

    public float angleStart() {
        return VecUtil.toYaw(p1.subtract(ctrl1)) + 180;
    }

    public List<CubicCurve> subsplit(int maxSize) {
        List<CubicCurve> res = new ArrayList<>();
        if (p1.distanceTo(p2) <= maxSize) {
            res.add(this);
        } else {
            res.addAll(this.truncate(0.5).subsplit(maxSize));
            res.addAll(this.reverse().truncate(0.5).reverse().subsplit(maxSize));
        }
        return res;
    }


    public CubicCurve linearize(TrackSmoothing smoothing,float pitchStart,float pitchEnd) {
        double start = p1.distanceTo(ctrl1);
        double middle = ctrl1.distanceTo(ctrl2);
        double end = ctrl2.distanceTo(p2);

        double lengthGuess = start + middle + end;
        double height = p2.y - p1.y;

        switch (smoothing) {
            case NEITHER:
                return new CubicCurve(
                        p1,
                        ctrl1.add(0, (start / lengthGuess) * height, 0),
                        ctrl2.add(0, -(end / lengthGuess) * height, 0),
                        p2
                );
            case NEAR:
                return new CubicCurve(
                        p1,
                        ctrl1,
                        ctrl2.add(0, -(end / (middle + end)) * height, 0),
                        p2
                );
            case FAR:
                return new CubicCurve(
                        p1,
                        ctrl1.add(0, (start / (start + middle)) * height, 0),
                        ctrl2,
                        p2
                );
            case CUSTOM:
                // 1. 原切线方向（水平投影单位向量）
                Vec3d tanStart = ctrl1.subtract(p1);          // 原手柄
                Vec3d tanEnd   = p2.subtract(ctrl2);

                double hLenStart = Math.hypot(tanStart.x, tanStart.z);
                double hLenEnd   = Math.hypot(tanEnd.x, tanEnd.z);

                Vec3d hDirStart = hLenStart > 0
                        ? new Vec3d(tanStart.x / hLenStart, 0, tanStart.z / hLenStart)
                        : new Vec3d(1, 0, 0);   // 退化时给一个默认方向
                Vec3d hDirEnd   = hLenEnd > 0
                        ? new Vec3d(tanEnd.x / hLenEnd, 0, tanEnd.z / hLenEnd)
                        : new Vec3d(1, 0, 0);

                // 2. 按俯仰角合成新手柄
                double vyStart = Math.tan(Math.toRadians(pitchStart)) *
                        Math.hypot(tanStart.x, tanStart.z);
                double vyEnd   = Math.tan(Math.toRadians(pitchEnd)) *
                        Math.hypot(tanEnd.x, tanEnd.z);

                Vec3d newCtrl1 = p1.add(new Vec3d(
                        hDirStart.x * hLenStart,
                        vyStart,
                        hDirStart.z * hLenStart));
                Vec3d newCtrl2 = p2.subtract(new Vec3d(
                        hDirEnd.x * hLenEnd,
                        vyEnd,
                        hDirEnd.z * hLenEnd));

                return new CubicCurve(p1, newCtrl1, newCtrl2, p2);

            case BOTH: default:
                return this;
        }
    }
}
