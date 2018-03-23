package cam72cam.immersiverailroading.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class BurnUtil {
	private static Map<String, Integer> info = new HashMap<String, Integer>();
	
	static {
		// BC
		info.put("oil", 100);
		info.put("oil_heavy", 70);
		info.put("oil_dense", 110);
		info.put("oil_distilled", 50);
		info.put("fuel_dense", 110);
		info.put("fuel_mixed_heavy", 130);
		info.put("fuel_light", 150);
		info.put("fuel_mixed_light", 100);
		// IE/IP
		info.put("diesel", 200);
		info.put("biodiesel", 170);
		info.put("biofuel", 170);
		info.put("ethanol", 170);
		info.put("gasoline", 100);
	}
	
	public static int getBurnTime(ItemStack stack) {
		return TileEntityFurnace.getItemBurnTime(stack);
	}
	
	public static int getBurnTime(Fluid fluid) {
		if (info.containsKey(fluid.getName())) {
			return info.get(fluid.getName());
		}
		return 0;
	}
	public static List<Fluid> burnableFluids() {
		List<Fluid> values = new ArrayList<Fluid>();
		for (String name : info.keySet()) {
			Fluid found = FluidRegistry.getFluid(name);
			if (found != null) {
				values.add(found);
			}
		}
		return values;
	}
}
