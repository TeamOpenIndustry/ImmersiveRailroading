package cam72cam.immersiverailroading.util;

import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;

@TagMapped(Speed.TagMapper.class)
public class Speed {
	// 20 tps * 3.6km/h
	private static final double speedRatio = 20 * 3.6;
	
	public static final Speed ZERO = fromMetric(0);
	
	private double internalSpeed;
	
	public static Speed fromMinecraft(double speed) {
		return new Speed(speed);
	}
	
	public static Speed fromMetric(double speed) {
		return new Speed(speed / speedRatio);
	}
	
	private Speed(double speed) {
		internalSpeed = speed;
	};
	
	public double minecraft() {
		return internalSpeed;
	}
	
	public double metric() {
		return internalSpeed * speedRatio;
	}

	public double imperial() {
		return metric() * 0.621371;
	}

	public String metricString() {
		return String.format("%.2f km/h", metric());
	}

	public boolean isZero() {
		return internalSpeed == 0;
	}

	public static class TagMapper implements cam72cam.mod.serialization.TagMapper<Speed> {
		@Override
		public TagAccessor<Speed> apply(Class<Speed> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, o) -> d.setDouble(fieldName, o.internalSpeed),
					(d) -> new Speed(d.getDouble(fieldName))
			);
		}
	}
}
