package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.immersiverailroading.util.RollAndOffsetInfo;
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

    // Used for subSplit rollAndOffsetInfo
    public double arcLenFactorStart;
    public double arcLenFactorEnd;

    //http://spencermortensen.com/articles/bezier-circle/
    public final static double c = 0.55191502449;

    public CubicCurve(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2) {
        this.p1 = p1;
        this.ctrl1 = ctrl1;
        this.ctrl2 = ctrl2;
        this.p2 = p2;
    }

    public CubicCurve(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2, double arcLenFactorStart, double arcLenFactorEnd) {
        this(p1, ctrl1, ctrl2, p2);
        this.arcLenFactorStart = arcLenFactorStart;
        this.arcLenFactorEnd = arcLenFactorEnd;
    }

    // This algorithm expands handle wrongly, which causes worse approximation, whose error can arrive 0.4 with R = 100.
    // Legacy quarter-circle approximation retained for compatibility, really not suggested.
    public static CubicCurve circleSimpleMortensen(int radius, float degrees, double arcLenFactorStart, double arcLenFactorEnd) {
        float cRadScale = degrees / 90;
        Vec3d p1 = new Vec3d(0, 0, radius);
        Vec3d ctrl1 = new Vec3d(cRadScale * c * radius, 0, radius);
        Vec3d ctrl2 = new Vec3d(radius, 0, cRadScale * c * radius);
        Vec3d p2 = new Vec3d(radius, 0, 0);

        Matrix4 rotation = new Matrix4();
        rotation.rotate(Math.toRadians(-90 + degrees), 0, 1, 0);

        return new CubicCurve(p1, ctrl1, rotation.apply(ctrl2), rotation.apply(p2), arcLenFactorStart, arcLenFactorEnd).apply(new Matrix4().translate(0, 0, -radius));
    }

    // According to Matlab test, this could be a little worse with angle in 0-80, and a little better with 80-90 than circleClassic, but very close
    public static CubicCurve circleBetterMortensen(int radius, float degrees, double arcLenFactorStart, double arcLenFactorEnd) {
        float cRadScale = (float) (Math.tan(Math.toRadians(degrees / 90)) / Math.tan(Math.toRadians(90f / 4f)));
        Vec3d p1 = new Vec3d(0, 0, radius);
        Vec3d ctrl1 = new Vec3d(cRadScale * c * radius, 0, radius);
        Vec3d ctrl2 = new Vec3d(radius, 0, cRadScale * c * radius);
        Vec3d p2 = new Vec3d(radius, 0, 0);

        Matrix4 rotation = new Matrix4();
        rotation.rotate(Math.toRadians(-90 + degrees), 0, 1, 0);

        return new CubicCurve(p1, ctrl1, rotation.apply(ctrl2), rotation.apply(p2), arcLenFactorStart, arcLenFactorEnd).apply(new Matrix4().translate(0, 0, -radius));
    }

    // According to Matlab test, classic algorithm still has better approximation.
    public static CubicCurve circleClassic(double radius, double degrees, double arcLenFactorStart, double arcLenFactorEnd) {
        double theta = Math.toRadians(degrees);
        double handle = radius * 4.0 / 3.0 * Math.tan(theta / 4.0);

        Vec3d p1 = new Vec3d(0, 0, radius);
        Vec3d ctrl1 = new Vec3d(handle, 0, radius);
        Vec3d p2 = new Vec3d(radius * Math.sin(theta), 0, radius * Math.cos(theta));
        Vec3d ctrl2 = new Vec3d(p2.x - handle * Math.cos(theta), 0, p2.z + handle * Math.sin(theta));

        return new CubicCurve(p1, ctrl1, ctrl2, p2, arcLenFactorStart, arcLenFactorEnd).apply(new Matrix4().translate(0, 0, -radius));
    }

    // https://help.autodesk.com/view/CIV3D/2025/ENU/?guid=GUID-DD7C0EA1-8465-45BA-9A39-FC05106FD822
    // cubicParabolaMaxAngle = Math.toDegrees(Math.atan(1.0/Math.sqrt(5))) = 24.09484255211;
    public static double cubicParabolaMaxAngle = 24.09484255211;// ease3Parabola will meet min R at this angle

    /**
     * Constructs the exact Bézier representation of a cubic parabola
     * between a straight line and a circular arc.
     *
     * <p>The generated Bézier curve is mathematically identical to the
     * corresponding cubic parabola segment rather than an approximation.</p>
     *
     * @param radius Radius of the circular arc.
     * @param projectedLength Projection length along the local x-axis.
     * @param straightAtP1 {@code true} for Straight→Circular,
     *                     {@code false} for Circular→Straight.
     */
    public static CubicCurve cubicParabola(double radius,
                                           double projectedLength,
                                           boolean straightAtP1) {

        double projectedLength2 = projectedLength * projectedLength;

        Vec3d p1, ctrl1, ctrl2, p2;

        if (straightAtP1) {

            p1 = new Vec3d(0, 0, 0);

            // Uniform x-spacing makes x(t) exactly linear.
            ctrl1 = new Vec3d(projectedLength / 3.0, 0, 0);
            ctrl2 = new Vec3d(projectedLength * 2.0 / 3.0, 0, 0);

            // Exact cubic parabola endpoint.
            p2 = new Vec3d(
                    projectedLength,
                    0,
                    -projectedLength2 / (6.0 * radius));

            return new CubicCurve(p1, ctrl1, ctrl2, p2);

        } else {

            p1 = new Vec3d(0, 0, 0);

            ctrl1 = new Vec3d(
                    projectedLength / 3.0,
                    0,
                    projectedLength2 / (6.0 * radius));

            ctrl2 = new Vec3d(
                    projectedLength * 2.0 / 3.0,
                    0,
                    projectedLength2 / (6.0 * radius));

            p2 = new Vec3d(
                    projectedLength,
                    0,
                    projectedLength2 / (6.0 * radius));

            Matrix4 rotation = new Matrix4();
            rotation.rotate(
                    Math.atan(projectedLength / (2.0 * radius)),
                    0, 1, 0);

            return new CubicCurve(
                    rotation.apply(p1),
                    rotation.apply(ctrl1),
                    rotation.apply(ctrl2),
                    rotation.apply(p2));
        }
    }

    /**
     * Constructs the exact Bézier representation of a cubic parabola
     * between a straight line and a circular arc from the specified
     * deflection angle.
     *
     * @param radius Radius of the circular arc.
     * @param angleDeg Total deflection angle in degrees.
     * @param straightAtP1 {@code true} for Straight→Circular,
     *                     {@code false} for Circular→Straight.
     */
    public static CubicCurve cubicParabolaByAngle(double radius,
                                                  double angleDeg,
                                                  boolean straightAtP1) {
        double projectedLength =
                2.0 * radius * Math.tan(Math.toRadians(angleDeg));

        return cubicParabola(radius, projectedLength, straightAtP1);
    }

    /**
     * Returns whether a cubic parabola can be constructed between two
     * circular radii with the specified deflection angle.
     *
     * @param startRadius Radius at the beginning of the transition.
     * @param endRadius Radius at the end of the transition.
     * @param angleDeg Total deflection angle in degrees.
     * @return {@code true} if a valid cubic parabola exists.
     */
    public static boolean isCubicParabolaValid(double startRadius, double endRadius, double angleDeg) {
        if (Double.isNaN(startRadius) || Double.isNaN(endRadius) || Double.isNaN(angleDeg))
            return false;

        if (startRadius <= 0 || endRadius <= 0)
            return false;

        if (angleDeg < 0)
            return false;

        // Degenerate case.
        if (startRadius == endRadius)
            return angleDeg == 0;

        if (endRadius < startRadius) {
            double tmp = startRadius;
            startRadius = endRadius;
            endRadius = tmp;
        }

        double k = startRadius / endRadius;
        double tanAngle = Math.tan(Math.toRadians(angleDeg));

        double k2 = k * k;
        double delta = (1.0 - k2) * (1.0 - k2)
                - 4.0 * k2 * tanAngle * tanAngle;

        return delta >= 0.0;
    }

    /**
     * Solves the projected length of a cubic parabola from the specified
     * radii and total deflection angle.
     *
     * <p>The returned length is the projection of the cubic parabola onto
     * the local x-axis rather than the true arc length.</p>
     *
     * @param startRadius Radius at the beginning of the transition.
     * @param endRadius Radius at the end of the transition.
     * @param angleDeg Total deflection angle in degrees.
     * @return Projected length of the cubic parabola.
     * @throws IllegalArgumentException if the specified parameters cannot
     *                                  construct a valid cubic parabola.
     */
    private static double solveProjectedLength(double startRadius,
                                               double endRadius,
                                               double angleDeg) {
        if (!isCubicParabolaValid(startRadius, endRadius, angleDeg))
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot construct cubic parabola (R1=%.3f, R2=%.3f, angle=%.3f°)",
                            startRadius, endRadius, angleDeg
                    )
            );

        if (endRadius < startRadius) {
            double tmp = startRadius;
            startRadius = endRadius;
            endRadius = tmp;
        }

        double k = startRadius / endRadius;
        double k2 = k * k;

        double tanAngle = Math.tan(Math.toRadians(angleDeg));

        double delta =
                (1.0 - k2) * (1.0 - k2)
                        - 4.0 * k2 * tanAngle * tanAngle;

        double tanStart =
                ((1.0 - k2) - Math.sqrt(delta))
                        / (2.0 * k2 * tanAngle);

        return 2.0 * startRadius * tanStart;
    }

    //    Matlab Script:
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

    /**
     * Constructs the exact Bézier representation of a cubic parabola
     * between two circular arcs.
     *
     * <p>The generated Bézier curve is mathematically identical to the
     * corresponding cubic parabola segment rather than an approximation.</p>
     *
     * @param startRadius Radius at the beginning of the transition.
     * @param endRadius Radius at the end of the transition.
     * @param projectedLength Projection length along the local x-axis.
     */
    public static CubicCurve cubicParabola(double startRadius,
                                           double endRadius,
                                           double projectedLength) {
        boolean largerRadiusAtP1 = startRadius > endRadius;

        if (largerRadiusAtP1) {
            double tmp = startRadius;
            startRadius = endRadius;
            endRadius = tmp;
        }

        double k = startRadius / endRadius;
        double k2 = k * k;
        double k3 = k2 * k;

        double projectedLength2 = projectedLength * projectedLength;

        double handleLength = projectedLength * (1.0 - k) / 3.0;
        double handleProjection = handleLength * projectedLength;

        Vec3d p1, ctrl1, ctrl2, p2;

        Matrix4 rotation = new Matrix4();

        if (!largerRadiusAtP1) {

            // Small radius -> Large radius

            p1 = new Vec3d(
                    k * projectedLength,
                    0,
                    k3 * projectedLength2 / (6.0 * startRadius));

            ctrl1 = new Vec3d(
                    k * projectedLength + handleLength,
                    0,
                    k3 * projectedLength2 / (6.0 * startRadius)
                            + k2 * handleProjection / (2.0 * startRadius));

            ctrl2 = new Vec3d(
                    k * projectedLength + handleLength * 2.0,
                    0,
                    projectedLength2 / (6.0 * startRadius)
                            - handleProjection / (2.0 * startRadius));

            p2 = new Vec3d(
                    projectedLength,
                    0,
                    projectedLength2 / (6.0 * startRadius));

            // Translate the local origin to P1.
            double originX = p1.x;
            double originZ = p1.z;

            p1 = p1.add(-originX, 0, -originZ);
            ctrl1 = ctrl1.add(-originX, 0, -originZ);
            ctrl2 = ctrl2.add(-originX, 0, -originZ);
            p2 = p2.add(-originX, 0, -originZ);

            // Flip the local z-axis.
            ctrl1 = ctrl1.add(0, 0, -2.0 * ctrl1.z);
            ctrl2 = ctrl2.add(0, 0, -2.0 * ctrl2.z);
            p2 = p2.add(0, 0, -2.0 * p2.z);

            rotation.rotate(
                    -Math.atan(projectedLength * k / (2.0 * endRadius)),
                    0, 1, 0);

        } else {

            // Large radius -> Small radius

            p1 = new Vec3d(
                    0,
                    0,
                    0);

            ctrl1 = new Vec3d(
                    handleLength,
                    0,
                    handleProjection / (2.0 * startRadius));

            ctrl2 = new Vec3d(
                    handleLength * 2.0,
                    0,
                    (1.0 - k3) * projectedLength2 / (6.0 * startRadius)
                            - k2 * handleProjection / (2.0 * startRadius));

            p2 = new Vec3d(
                    projectedLength * (1.0 - k),
                    0,
                    (1.0 - k3) * projectedLength2 / (6.0 * startRadius));

            rotation.rotate(
                    Math.atan(projectedLength / (2.0 * startRadius)),
                    0, 1, 0);
        }

        return new CubicCurve(
                rotation.apply(p1),
                rotation.apply(ctrl1),
                rotation.apply(ctrl2),
                rotation.apply(p2));
    }

    /**
     * Constructs the exact Bézier representation of a cubic parabola
     * between two circular arcs.
     *
     * <p>The generated Bézier curve is mathematically identical to the
     * corresponding cubic parabola segment rather than an approximation.</p>
     *
     * @param startRadius Radius at the beginning of the transition.
     * @param endRadius Radius at the end of the transition.
     * @param angleDeg Total deflection angle in degrees.
     * @return Cubic Bézier representation of the cubic parabola.
     * @throws IllegalArgumentException if the specified parameters cannot
     *                                  construct a valid cubic parabola.
     */
    public static CubicCurve cubicParabolaByAngle(double startRadius,
                                           double endRadius,
                                           double angleDeg) {
        return cubicParabola(
                startRadius,
                endRadius,
                solveProjectedLength(startRadius, endRadius, angleDeg)
        );
    }

    public CubicCurve apply(Matrix4 mat) {
        return new CubicCurve(
                mat.apply(p1),
                mat.apply(ctrl1),
                mat.apply(ctrl2),
                mat.apply(p2),
                arcLenFactorStart, arcLenFactorEnd
        );
    }

    public CubicCurve reverse() {
        return new CubicCurve(p2, ctrl2, ctrl1, p1, arcLenFactorEnd, arcLenFactorStart);
    }

    /**
     *  DeCasteljau algorithm,
     *  return left part (0 ~ t) of CubicCurve
     * */
    public CubicCurve getLeft(double t) {
        Vec3d q0 = lerp(p1, ctrl1, t);
        Vec3d q1 = lerp(ctrl1, ctrl2, t);
        Vec3d q2 = lerp(ctrl2, p2, t);

        Vec3d r0 = lerp(q0, q1, t);
        Vec3d r1 = lerp(q1, q2, t);

        Vec3d s = lerp(r0, r1, t);

        double localRatio = lengthInBetween(0, t, 10) / lengthInBetween(0, 1, 10);
        double globalEnd = arcLenFactorStart + localRatio * (arcLenFactorEnd - arcLenFactorStart);
        return new CubicCurve(p1, q0, r0, s, arcLenFactorStart, globalEnd);
    }

    private Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return a.scale(1 - t).add(b.scale(t));
    }



    public Pair<CubicCurve, CubicCurve> split(double t) {
        return Pair.of(this.getLeft(t), this.reverse().getLeft(1-t));
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

    public List<PosRollOffset> toList(double stepSize, RollAndOffsetInfo rollAndOffsetInfo) {//rollAndOffsetInfo is nullable
        List<PosRollOffset> result = new ArrayList<>();
        result.add(new PosRollOffset(
                p1,
                rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getRoll(0),
                rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getYOffset(0),
                rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getZOffset(0)
        ));
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
                double mid = (low + high) / 2;//this is a t value not length value!

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

                double scale = (mid - t[i]) / (t[i+1] - t[i]);
                double arcLen = len[i] + (len[i+1] - len[i]) * scale;
                double l = arcLen / len[segment];

                result.add(new PosRollOffset(
                        position(mid),
                        rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getRoll(l),
                        rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getYOffset(l),
                        rollAndOffsetInfo == null ? 0 : rollAndOffsetInfo.getZOffset(l)
                ));
                lastLength = currentLen + lengthInBetween(low, mid, 10);
            }
        }

        if(len[segment] - lastLength >= 0.8 * stepSize){
            result.add(new PosRollOffset(
                    p2,
                    rollAndOffsetInfo==null ? 0 : rollAndOffsetInfo.getRoll(1),
                    rollAndOffsetInfo==null ? 0 : rollAndOffsetInfo.getYOffset(1),
                    rollAndOffsetInfo==null ? 0 : rollAndOffsetInfo.getZOffset(1)
            ));
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
            res.addAll(this.getLeft(0.5).subsplit(maxSize));
            res.addAll(this.reverse().getLeft(0.5).reverse().subsplit(maxSize));
        }
        return res;
    }


    @Deprecated
    public CubicCurve linearize(TrackSmoothing smoothing) {//TODO: Remove track smoothing and only use pitch-locked
        double start = p1.distanceTo(ctrl1);
        double middle = ctrl1.distanceTo(ctrl2);
        double end = ctrl2.distanceTo(p2);

        double lengthGuess = start + middle + end;
        double height = p2.y - p1.y;

        return switch (smoothing) {
            case NEITHER -> new CubicCurve(
                    p1,
                    ctrl1.add(0, (start / lengthGuess) * height, 0),
                    ctrl2.add(0, -(end / lengthGuess) * height, 0),
                    p2,
                    arcLenFactorStart, arcLenFactorEnd
            );
            case NEAR -> new CubicCurve(
                    p1,
                    ctrl1,
                    ctrl2.add(0, -(end / (middle + end)) * height, 0),
                    p2,
                    arcLenFactorStart, arcLenFactorEnd
            );
            case FAR -> new CubicCurve(
                    p1,
                    ctrl1.add(0, (start / (start + middle)) * height, 0),
                    ctrl2,
                    p2,
                    arcLenFactorStart, arcLenFactorEnd
            );
            default -> this;
        };
    }
}
