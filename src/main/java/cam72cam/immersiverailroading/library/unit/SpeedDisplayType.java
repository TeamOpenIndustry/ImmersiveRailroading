package cam72cam.immersiverailroading.library.unit;

public enum SpeedDisplayType {
	kmh,
	ms,
	mph;

	public double convertFromKmh(double value) {
		switch (this) {
			default:
			case kmh:
				return value;
			case ms:
				return value / 3.6;
			case mph:
				return value * 0.621371;
		}
	}

	public String toUnitString() {
		switch (this) {
			default:
			case kmh:
				return "km/h";
			case ms:
				return "m/s";
			case mph:
				return "mph";
		}
	}
}
