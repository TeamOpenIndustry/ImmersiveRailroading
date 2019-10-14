package cam72cam.immersiverailroading.util;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.mod.fluid.Fluid;

public class LiquidUtil {
	public static List<Fluid> getWater() {
		List<Fluid> filter = new ArrayList<>();
		for (String fluid : Config.ConfigBalance.waterTypes) {
			if (Fluid.getFluid(fluid) != null) {
				filter.add(Fluid.getFluid(fluid));
			}
		}
		return filter;
	}
}
