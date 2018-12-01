package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CubicCurve {
    public final Vec3d p1;
    public final Vec3d ctrl1;
    public final Vec3d ctrl2;
    public final Vec3d p2;

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
        Vec3d pt = Vec3d.ZERO;
        pt = pt.add(p1.		scale(1 * Math.pow(1-t, 3) * Math.pow(t, 0)));
        pt = pt.add(ctrl1.	scale(3 * Math.pow(1-t, 2) * Math.pow(t, 1)));
        pt = pt.add(ctrl2.	scale(3 * Math.pow(1-t, 1) * Math.pow(t, 2)));
        pt = pt.add(p2.		scale(1 * Math.pow(1-t, 0) * Math.pow(t, 3)));
        return pt;
    }

    public List<Vec3d> toList(double stepSize) {
        List<Vec3d> res = new ArrayList<>();
        List<Vec3d> resRev = new ArrayList<>();
        res.add(p1);
        if (p1.equals(p2)) {
            return res;
        }

        resRev.add(p2);
        double precision = 5;

        double t = 0;
        while (t <= 0.5) {
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

        double lt = t;
        t = 1;

        while (t > lt) {
            for (double i = 1; i < precision; i++) {
                Vec3d prev = resRev.get(resRev.size()-1);

                double delta = (Math.pow(10, -i));

                for (;t > lt - delta; t-=delta) {
                    Vec3d pos = position(t);
                    if (pos.distanceTo(prev) > stepSize) {
                        // We passed it, just barely
                        t += delta;
                        break;
                    }
                }
            }
            if (t > lt) {
                resRev.add(position(t));
            }
        }
        Collections.reverse(resRev);
        res.addAll(resRev);
        return res;
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
}
