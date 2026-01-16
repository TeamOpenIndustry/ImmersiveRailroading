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

    //https://help.autodesk.com/view/CIV3D/2025/ENU/?guid=GUID-DD7C0EA1-8465-45BA-9A39-FC05106FD822
//    public static double cubicParabolaMaxAngle = Math.toDegrees(Math.atan(1.0/Math.sqrt(5)));
    public static double cubicParabolaMaxAngle = 24.09484255211;//ease3Parabola will meet min R at this angle

    //what should we store? angle and radius so this will match existing nbt storage?
    public static CubicCurve cubicParabola(double Radius, double Len, boolean straightAtP1) {
        double Len2 = Len * Len;
        Vec3d p1, ctrl1, ctrl2, p2;
        if (straightAtP1) {          //直线→缓和→圆
            p1 = new Vec3d(0, 0, 0);
            ctrl1 = new Vec3d(Len / 3.0, 0, 0);
            ctrl2 = new Vec3d(2 * Len / 3.0, 0, 0);
            p2 = new Vec3d(Len, 0, -Len2 / (6 * Radius));//these seems the best?

            return new CubicCurve(p1, ctrl1, ctrl2, p2);
        } else {                     //圆→缓和→直线
            p1 = new Vec3d(0, 0, 0);
            ctrl1 = new Vec3d(Len / 3.0, 0, Len2 / (6 * Radius));
            ctrl2 = new Vec3d(2 * Len / 3.0, 0, Len2 / (6 * Radius));
            p2 = new Vec3d(Len, 0, Len2 / (6 * Radius));

            Matrix4 quart = new Matrix4();
            quart.rotate(Math.atan(0.5*Len/Radius), 0, 1, 0);

            return new CubicCurve(p1, quart.apply(ctrl1), quart.apply(ctrl2), quart.apply(p2));
        }
    }

    public static CubicCurve cubicParabolaAngle(double Radius, double angleDeg, boolean straightAtP1) {
        double Len = 2 * Radius * Math.tan(Math.toRadians(angleDeg));
        return cubicParabola(Radius, Len, straightAtP1);
    }

    public static CubicCurve cubicParabolaR1R2Angle(double Radius, double angleDeg, double nextRadius) {
        //warning:angleDeg must be bigger than this.cubicParabolaMaxAngle, or Len will turn to NaN and stack overflow error will be thrown!
        boolean shouldLocateAtP2 = nextRadius < Radius;
        if(nextRadius < Radius){
            double t = Radius;
            Radius = nextRadius;
            nextRadius = t;
        }
        double k = Radius/nextRadius;

        double tanAngleDeg = Math.tan(Math.toRadians(angleDeg));
        double k2 = k * k;
        double delta = (1 - k2) * (1 - k2) - 4 * k2 * tanAngleDeg * tanAngleDeg;
        double tanAtRadius = ( (1 - k2) - Math.sqrt(delta) ) / (2 * k2 * tanAngleDeg);//delta is dangerous
        double len = 2*Radius*tanAtRadius;

        return shouldLocateAtP2?cubicParabolaR1R2Len(nextRadius,Radius,len):cubicParabolaR1R2Len(Radius,nextRadius,len);
    }
    public static boolean isCubicParabolaDeltaValid(double Radius, double angleDeg, double nextRadius){
        if(nextRadius < Radius){
            double t = Radius;
            Radius = nextRadius;
            nextRadius = t;
        }
        double k = Radius/nextRadius;
        double tanAngleDeg = Math.tan(Math.toRadians(angleDeg));
        double k2 = k * k;
        double delta = (1 - k2) * (1 - k2) - 4 * k2 * tanAngleDeg * tanAngleDeg;
        return delta >= 0;
    }
//    R = 300;
//    R2 = 1000;
//    k = R/R2;
//    L = 40;
//    piece = L*(1-k)/3;
//    a = 1/(6*R*L);
//
//    x = linspace(0,L,500);
//    y_th = a*x.^3;
//
//    P0 = [k*L k^3*L^2/(6*R)];
//    P1 = [k*L+piece k^3*L^2/(6*R)+piece*k^2*L/(2*R)];
//    P2 = [k*L+piece*2  L^2/(6*R)-piece*L/(2*R)];
//    P3 = [L      L^2/(6*R)];
    public static CubicCurve cubicParabolaR1R2Len(double Radius, double nextRadius, double Len) {
        boolean shouldLocateAtP2 = nextRadius < Radius;
        if(nextRadius < Radius){
            double t = Radius;
            Radius = nextRadius;
            nextRadius = t;
        }
        double k = Radius/nextRadius;
        double piece = Len * (1-k)/3;
        double k2 = k * k;
        double k3 = k * k * k;
        double Len2 = Len * Len;
        double PL = piece * Len;

        Vec3d p1, ctrl1, ctrl2, p2;
        Matrix4 quart = new Matrix4();

        if(!shouldLocateAtP2){
            p1 = new Vec3d(k*Len, 0, k3*Len2/(6*Radius));
            ctrl1 = new Vec3d(k*Len+piece, 0, k3*Len2/(6*Radius) + k2*PL/(2*Radius));
            ctrl2 = new Vec3d(k*Len+piece*2, 0, Len2/(6*Radius) - PL/(2*Radius));
            p2 = new Vec3d(Len, 0, Len2/(6*Radius));
            //translate
            double dx = p1.x;
            double dz = p1.z;
            p1 = p1.add(-dx,0,-dz);
            p2 = p2.add(-dx,0,-dz);
            ctrl1 = ctrl1.add(-dx,0,-dz);
            ctrl2 = ctrl2.add(-dx,0,-dz);
            //mirror
            p2 = p2.add(0,0,-p2.z*2);
            ctrl1 = ctrl1.add(0,0,-ctrl1.z*2);
            ctrl2 = ctrl2.add(0,0,-ctrl2.z*2);

            quart.rotate(-Math.atan(0.5*Len*k/nextRadius), 0, 1, 0);
        }else{
            p1 = new Vec3d(0,0,0);
            ctrl1 = new Vec3d(piece, 0, PL/(2*Radius));
            ctrl2 = new Vec3d(piece*2, 0, (1-k3)*Len2/(6*Radius)-k2*PL/(2*Radius));
            p2 = new Vec3d(Len*(1-k), 0, (1-k3)*Len2/(6*Radius));

            quart.rotate(Math.atan(0.5*Len/Radius), 0, 1, 0);
        }
        return new CubicCurve(quart.apply(p1), quart.apply(ctrl1), quart.apply(ctrl2), quart.apply(p2));
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
            case PITCH_LOCKED_CUBIC:
                return pitchLockedLiner(p1, ctrl1, ctrl2, p2, pitchStart, pitchEnd);
            case BOTH: default:
                return this;
        }
    }

    private static CubicCurve pitchLockedLiner(
            Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2,
            float pitchStart, float pitchEnd
    ) {
        pitchStart = (float) Math.atan(pitchStart/1000f);
        pitchEnd = (float) Math.atan(pitchEnd/1000f);

        Vec3d tanStart = ctrl1.subtract(p1);
        Vec3d tanEnd   = p2.subtract(ctrl2);
        double hLenStart = Math.hypot(tanStart.x, tanStart.z);
        double hLenEnd   = Math.hypot(tanEnd.x, tanEnd.z);
        Vec3d hDirStart = hLenStart > 0 ?
                new Vec3d(tanStart.x / hLenStart, 0, tanStart.z / hLenStart) :
                new Vec3d(1, 0, 0);
        Vec3d hDirEnd   = hLenEnd   > 0 ?
                new Vec3d(tanEnd.x   / hLenEnd,   0, tanEnd.z   / hLenEnd) :
                new Vec3d(1, 0, 0);

        double vyStart = Math.tan(pitchStart) * hLenStart;
        double vyEnd   = Math.tan(pitchEnd)   * hLenEnd;

        Vec3d newCtrl1 = p1.add(hDirStart.scale(hLenStart))
                .add(0, vyStart, 0);
        Vec3d newCtrl2 = p2.subtract(hDirEnd.scale(hLenEnd))
                .subtract(0, vyEnd, 0);
        return new CubicCurve(p1, newCtrl1, newCtrl2, p2);
    }
}
