package cam72cam.immersiverailroading.entity;

public class Speed {
	private static double speedRatio = 2;
	
	private double internalSpeed;
	
	public static Speed fromMinecraft(double speed) {
		return new Speed(speed);
	}
	
	public static Speed fromMetric(double speed) {
		return new Speed(speed / speedRatio / 10 / 3.6);
	}
	
	private Speed(double speed) {
		internalSpeed = speed;
	};
	
	public double minecraft() {
		return internalSpeed;
	}
	
	public double metric() {
		return internalSpeed * speedRatio * 10 * 3.6;
	}

	public String metricString() {
		return String.format("%.2f km/h", metric());
	}
}
