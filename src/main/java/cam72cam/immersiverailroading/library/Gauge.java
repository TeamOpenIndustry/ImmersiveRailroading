package cam72cam.immersiverailroading.library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.text.TextUtil;
import trackapi.lib.Gauges;

@TagMapped(Gauge.TagMapper.class)
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
	    return TextUtil.translate("immersiverailroading:gauge." + name.toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Gauge && ((Gauge) o).gauge == this.gauge;
	}

	public boolean isModel() {
		return value() < 0.3;
	}

	public static List<Gauge> values() {
		return new ArrayList<>(gauges);
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

	public static Gauge standard() {
		return from(STANDARD);
	}

	public boolean shouldSit() {
		return this.gauge <= Gauges.MINECRAFT;
	}

	static class TagMapper implements cam72cam.mod.serialization.TagMapper<Gauge> {
		@Override
		public TagAccessor<Gauge> apply(Class<Gauge> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, g) -> d.setDouble(fieldName, g == null ? null : g.value()),
					d -> d.hasKey(fieldName) ? Gauge.from(d.getDouble(fieldName)) : null
			);
		}
	}
}
