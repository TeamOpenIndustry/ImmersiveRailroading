package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJFace;

import java.util.List;

public class MathUtil {
	public static double gradeToRadians(double grade) {
		return Math.atan2(grade, 100);
	}

	public static double gradeToDegrees(double grade) {
		return Math.toDegrees(gradeToRadians(grade));
	}

	//Java's built in modulus gives negative results on negative input for some reason
	//this results in screwey behavior when the coder is expecting the true math modulus
	//so I implemented that here
	public static double trueModulus(double val, double mod) {
		mod = Math.abs(mod);
		double res = val % mod;
		if(res != 0 && val < 0) {
			res += mod;
		}
		return res;
	}

	public static double deltaAngle(double source, double target) {
		return deltaMod(source, target, 360);
	}

	public static double deltaMod(double source, double target, double mod) {
		double a = target - source;
		a -= a > mod/2 ? mod : 0;
		a += a < -mod ? mod : 0;
		return a;
	}
	public static int deltaMod(int source, int target, int mod) {
		int a = target - source;
		a -= a > mod/2 ? mod : 0;
		a += a < -mod ? mod : 0;
		return a;
	}

	public static int gcd(int a, int b) {
		if (b == 0) {
			return a;
		}
		return gcd(b, a % b);
	}

	//Enough for now
	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}

	public static long clamp(long val, long min, long max) {
		return Math.max(min, Math.min(max, val));
	}

	public static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}

	public static Double intersectRayTriangle(Vec3d rayOrigin, Vec3d rayDir, OBJFace face) {
		final float EPSILON = 1e-6f;

		Vec3d edge1 = face.vertex1.pos.subtract(face.vertex0.pos);
		Vec3d edge2 = face.vertex2.pos.subtract(face.vertex0.pos);

		Vec3d h = rayDir.crossProduct(edge2);
		double a = edge1.dotProduct(h);

		if (Math.abs(a) < EPSILON) return null;

		double f = 1.0f / a;
		Vec3d s = rayOrigin.subtract(face.vertex0.pos);
		double u = f * s.dotProduct(h);

		if (u < 0.0f || u > 1.0f) return null;

		Vec3d q = s.crossProduct(edge1);
		double v = f * rayDir.dotProduct(q);

		if (v < 0.0f || u + v > 1.0f) return null;

		double t = f * edge2.dotProduct(q);

		return t >= 0 ? t : null;
	}

	public static Vec3d closestPointOnTriangle(Vec3d p, Vec3d p0, Vec3d p1, Vec3d p2) {
		Vec3d ab = p1.subtract(p0);
		Vec3d ac = p2.subtract(p0);
		Vec3d ap = p.subtract(p0);
		double d1 = ab.dotProduct(ap);
		double d2 = ac.dotProduct(ap);

		if (d1 <= 0f && d2 <= 0f) return p0;

		Vec3d bp = p.subtract(p1);
		double d3 = ab.dotProduct(bp);
		double d4 = ac.dotProduct(bp);
		if (d3 >= 0f && d4 <= d3) return p1;

		double vc = d1 * d4 - d3 * d2;
		if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
			double v = d1 / (d1 - d3);
			return p0.add(ab.scale(v));
		}

		Vec3d cp = p.subtract(p2);
		double d5 = ab.dotProduct(cp);
		double d6 = ac.dotProduct(cp);
		if (d6 >= 0f && d5 <= d6) return p2;

		double vb = d5 * d2 - d1 * d6;
		if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
			double w = d2 / (d2 -d6);
			return p0.add(ac.scale(w));
		}

		double va = d3 * d6 -d5 * d4;
		Vec3d bc = p2.subtract(p1);
		if (va <= 0f && (d4 - d3) >= 0.0 && (d5 - d6) >= 0f) {
			double w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
			return p1.add(bc.scale(w));
		}

		double denom = 1f / (va + vb + vc);
		double v = vb * denom;
		double w = vc  * denom;
		return p0.add(ab.scale(v)).add(ac.scale(w));
	}

	public static Vec3d closestPointOnSegmentXZ(Vec3d p, Vec3d a, Vec3d b) {
		double abx = b.x - a.x;
		double abz = b.z - a.z;
		double abLenSq = abx * abx + abz * abz;
		if (abLenSq < 1e-12) {
			return new Vec3d(a.x, p.y, a.z);
		}
		double t = ((p.x - a.x) * abx + (p.z - a.z) * abz) / abLenSq;
		t = Math.clamp(t, 0.0, 1.0);
		return new Vec3d(a.x + abx * t, p.y, a.z + abz * t);
	}
}
