package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;
import trackapi.lib.Util;

public enum Gauge {
	BRUNEL(2.14),
	STANDARD(Util.STANDARD_GAUGE),
	NARROW(0.9144),
	MINECRAFT(Util.MINECRAFT_GAUGE),
	MODEL(0.2),
	;
	
	private double gauge;

	Gauge(double gauge) {
		this.gauge = gauge;
	}
	
	public double value() {
		return gauge;
	}
	
	public double scale() {
		return gauge / STANDARD.value();
	}

	/**
	 * Returns the closest gauge
	 */
	public static Gauge from(double gauge) {
		Gauge result = Gauge.BRUNEL;
		for (Gauge g : values()) {
			if (g.gauge == gauge) {
				return g;
			}
			
			if (Math.abs(g.value() - gauge) < Math.abs(result.value() - gauge)) {
				result = g;
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:gauge." + super.toString().toLowerCase()); 
	}
}
