package cam72cam.immersiverailroading.library;

public enum SpeedDisplayType {
	kmh,
	mph,
	ms;

	public String toUnitString() {
		switch (this) {
			default:
			case kmh:
				return "km/h";
			case mph:
				return "mph";
			case ms:
				return "m/s";
		}
	}
}
