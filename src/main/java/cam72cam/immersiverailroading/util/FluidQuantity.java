package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import net.minecraftforge.fluids.Fluid;

public class FluidQuantity {
	public static final FluidQuantity ZERO = FromBuckets(0);
	private final int mb;
	
	private FluidQuantity(int mb) {
		this.mb = mb;
	}
	
	public static FluidQuantity FromBuckets(int buckets) {
		return new FluidQuantity(buckets * Fluid.BUCKET_VOLUME);
	}
	
	public static FluidQuantity FromLiters(int liters) {
		return new FluidQuantity(liters * ConfigBalance.MB_PER_LITER);
	}
	
	public static FluidQuantity FromMillibuckets(int mb) {
		return new FluidQuantity(mb);
	}
	
	public int Buckets() {
		return mb / Fluid.BUCKET_VOLUME;
	}
	
	public int Liters() {
		return mb / ConfigBalance.MB_PER_LITER;
	}
	
	public int MilliBuckets() {
		return mb;
	}

	public FluidQuantity scale(double scale) {
		return new FluidQuantity((int)Math.ceil(mb * scale));
	}

	public FluidQuantity min(FluidQuantity min) {
		if (min.mb > mb) {
			return min;
		}
		return this;
	}

	public FluidQuantity roundBuckets() {
		return FromBuckets((int)Math.ceil(mb/(double)Fluid.BUCKET_VOLUME));
	}
}
