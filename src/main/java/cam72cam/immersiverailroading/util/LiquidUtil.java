package cam72cam.immersiverailroading.util;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class LiquidUtil {
	public static List<Fluid> getWater() {
		List<Fluid> filter = new ArrayList<Fluid>();
		for (String fluid : Config.ConfigBalance.waterTypes) {
			if (FluidRegistry.getFluid(fluid) != null) {
				filter.add(FluidRegistry.getFluid(fluid));
			}
		}
		return filter;
	}
}
