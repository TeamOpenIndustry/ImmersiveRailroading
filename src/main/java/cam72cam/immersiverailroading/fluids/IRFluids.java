package cam72cam.immersiverailroading.fluids;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;

public class IRFluids {
	public static FluidSteam FLUID_STEAM = (FluidSteam) new FluidSteam(
			new ResourceLocation("immersiverailroading:blocks/fluid/steam_still"), 
			new ResourceLocation("immersiverailroading:blocks/fluid/steam_flow"));

	public static void registerFluids() {
		FluidRegistry.registerFluid(FLUID_STEAM);
        FluidRegistry.addBucketForFluid(FLUID_STEAM);
	}
}
