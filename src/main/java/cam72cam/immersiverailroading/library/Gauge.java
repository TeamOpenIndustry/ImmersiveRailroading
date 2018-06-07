package cam72cam.immersiverailroading.library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cam72cam.immersiverailroading.util.TextUtil;
import trackapi.lib.Gauges;

public class Gauge {
	public static final double STANDARD = Gauges.STANDARD;
	
	private final double gauge;
	private final String name;

	Gauge(double gauge, String name) {
		this.gauge = gauge;
		this.name = name;
	}
	
	public double value() {
		return gauge;
	}
	
	public double scale() {
		return gauge / Gauges.STANDARD;
	}
	
	@Override
	public String toString() {
	    return TextUtil.translate("immersiverailroading:gauge." + name.toLowerCase()); 
	}
	
	public boolean isModel() {
		return value() < 0.3;
	}

	public Gauge next() {
		boolean useNext = false;
		for (Gauge g : gauges) {
			if (useNext) {
				return g;
			}
			if (g.gauge == gauge) {
				useNext = true;
			}
		}
		return gauges.get(0);
	}	
	
	
	private static List<Gauge> gauges = new ArrayList<Gauge>();
	
	public static void reset() {
		gauges = new ArrayList<Gauge>();
	}
	
	public static void register(double gauge, String name) {
		remove(gauge);
		
		gauges.add(new Gauge(gauge, name));

		gauges.sort(new Comparator<Gauge>() {
			@Override
			public int compare(Gauge arg0, Gauge arg1) {
				return Double.compare(arg1.gauge, arg0.gauge);
			}
		});
	}
	
	public static void remove(double gauge) {
		for (int i = 0; i < gauges.size(); i ++) {
			if (gauges.get(i).value() == gauge) {
				gauges.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Returns the closest gauge
	 */
	public static Gauge from(double gauge) {
		Gauge closest = gauges.get(0);
		for (Gauge g : gauges) {
			if (g.gauge == gauge) {
				return g;
			}
			
			if (Math.abs(g.value() - gauge) < Math.abs(closest.value() - gauge)) {
				closest = g;
			}
		}
		return closest;
	}

	public boolean shouldSit() {
		return this.gauge <= Gauges.MINECRAFT;
	}
}
