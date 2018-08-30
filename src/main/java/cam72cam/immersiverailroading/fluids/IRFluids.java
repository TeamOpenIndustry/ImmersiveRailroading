package cam72cam.immersiverailroading.fluids;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class IRFluids {
	public static FluidSteam FLUID_STEAM = (FluidSteam) new FluidSteam(new ResourceLocation(ImmersiveRailroading.MODID + "steam_still"), new ResourceLocation(ImmersiveRailroading.MODID + "steam_flow"));

	public static void registerFluids() {
		
		Fluid fluid = FLUID_STEAM;
		
		FluidRegistry.registerFluid(fluid);
        {
            FluidRegistry.addBucketForFluid(fluid);
        }

	}
}
