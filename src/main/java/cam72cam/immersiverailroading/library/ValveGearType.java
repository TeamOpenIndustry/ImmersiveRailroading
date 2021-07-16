package cam72cam.immersiverailroading.library;

public enum ValveGearType {
	CONNECTING,
	STEPHENSON,
	WALSCHAERTS,
	HIDDEN,
	// TODO
	SHAY,
	CLIMAX,
	;

	public static ValveGearType from(String valveGear) {
		if (valveGear == null) {
			return null;
		}
		switch (valveGear) {
			case "TRI_WALSCHAERTS":
			case "GARRAT":
			case "MALLET_WALSCHAERTS":
				return WALSCHAERTS;
			case "T1":
				return STEPHENSON;
			default:
				return ValveGearType.valueOf(valveGear);
		}
	}
}