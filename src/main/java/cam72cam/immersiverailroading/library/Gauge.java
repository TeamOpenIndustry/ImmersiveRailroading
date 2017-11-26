package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.util.TextUtil;
import trackapi.lib.Gauges;

public enum Gauge {
	BRUNEL(2.14),
	STANDARD(Gauges.STANDARD),
	NARROW(0.9144),
	MINECRAFT(Gauges.MINECRAFT),
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
