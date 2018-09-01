package cam72cam.immersiverailroading.fluids;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public class FluidSteam extends Fluid {

	public final static String NAME = "steam";
	
   public FluidSteam(ResourceLocation still, ResourceLocation flowing) {
       super(NAME, still, flowing);
       
       setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
       setGaseous(true);
       setDensity(-1000);
       setViscosity(200);
       setTemperature(750);
       
   }

}
