package cam72cam.immersiverailroading.util;

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
}
