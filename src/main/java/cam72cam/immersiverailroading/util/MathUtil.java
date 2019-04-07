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
}
