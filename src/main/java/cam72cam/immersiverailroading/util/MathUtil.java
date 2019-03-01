package cam72cam.immersiverailroading.util;

public class MathUtil {
	public static double gradeToRadians(double grade) {
		return Math.atan2(grade, 100);
	}

	public static double gradeToDegrees(double grade) {
		return Math.toDegrees(gradeToRadians(grade));
	}
}
