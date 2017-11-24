package cam72cam.immersiverailroading.library;

import trackapi.lib.Util;

public enum SupportedTrackGauges {
	BRUNEL(2.14),
	STANDARD(Util.STANDARD_GAUGE),
	NARROW(0.9144),
	MINECRAFT(Util.MINECRAFT_GAUGE),
	MODEL(0.2),
	;
	
	private double gauge;

	SupportedTrackGauges(double gauge) {
		this.gauge = gauge;
	}
	
	public double get() {
		return gauge;
	}

	public static SupportedTrackGauges from(double gauge) {
		for (SupportedTrackGauges g : values()) {
			if (g.gauge == gauge) {
				return g;
			}
		}
		return STANDARD;
	}
}
