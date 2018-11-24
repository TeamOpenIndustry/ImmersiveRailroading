package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CubicCurve {
    private final Vec3d p1;
    private final Vec3d ctrl1;
    private final Vec3d ctrl2;
    private final Vec3d p2;

    //http://spencermortensen.com/articles/bezier-circle/
    public final static double c = 0.55191502449;

    public CubicCurve(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2) {
        this.p1 = p1;
        this.ctrl1 = ctrl1;
        this.ctrl2 = ctrl2;
        this.p2 = p2;
    }

    public CubicCurve(int radius) {
        this(
            new Vec3d(0, 0, radius - radius),
            new Vec3d(c * radius, 0, radius - radius),
            new Vec3d(radius, 0, c * radius - radius),
            new Vec3d(radius, 0, 0 - radius)
        );
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
        Vec3d pt = Vec3d.ZERO;
        pt = pt.add(p1.		scale(1 * Math.pow(1-t, 3) * Math.pow(t, 0)));
        pt = pt.add(ctrl1.	scale(3 * Math.pow(1-t, 2) * Math.pow(t, 1)));
        pt = pt.add(ctrl2.	scale(3 * Math.pow(1-t, 1) * Math.pow(t, 2)));
        pt = pt.add(p2.		scale(1 * Math.pow(1-t, 0) * Math.pow(t, 3)));
        return pt;
    }

    public List<Vec3d> toList(double stepSize) {
        List<Vec3d> res = new ArrayList<>();
        res.add(p1);
        double precision = 5;

        double t = 0;
        while (t <= 1) {
            for (double i = 1; i < precision; i++) {
                Vec3d prev = res.get(res.size()-1);

                double delta = (Math.pow(10, -i));

                for (;t < 1 + delta; t+=delta) {
                    Vec3d pos = position(t);
                    if (pos.distanceTo(prev) > stepSize) {
                        // We passed it, just barely
                        t -= delta;
                        break;
                    }
                }
            }
            res.add(position(t));
        }
        return res;
    }

    public List<Vec3d> toSplitList(double stepSize) {
        List<Vec3d> list = this.truncate(0.5).toList(stepSize);
        List<Vec3d> rev = this.reverse().truncate(0.5).toList(stepSize);
        Collections.reverse(rev);
        if (list.size() > 3 && rev.size() > 3) {
            Vec3d listEnd = list.get(list.size() - 3);
            Vec3d revEnd = rev.get(2);
            if (rev.get(0).distanceTo(listEnd) < list.get(list.size() - 1).distanceTo(listEnd)) {
                rev.remove(0);
            }
            if (rev.get(0).distanceTo(revEnd) > list.get(list.size() - 1).distanceTo(revEnd)) {
                list.remove(list.size() - 1);
            }
        }
        list.addAll(rev);
        return list;
    }

    public float angleStop() {
        return VecUtil.toYaw(p2.subtract(ctrl2));
    }

    public float angleStart() {
        return VecUtil.toYaw(p1.subtract(ctrl1)) + 180;
    }
}
