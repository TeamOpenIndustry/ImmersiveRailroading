package cam72cam.mod.fluid;

import net.minecraftforge.fluids.FluidRegistry;

import java.util.HashMap;
import java.util.Map;

public class Fluid {
    private static Map<String, Fluid> registryCache = new HashMap<>();

    public static final int BUCKET_VOLUME = net.minecraftforge.fluids.Fluid.BUCKET_VOLUME;
    public static final Fluid WATER = getFluid("water");
    public final String ident;
    public final net.minecraftforge.fluids.Fluid internal;


    private Fluid(String ident) {
        this.ident = ident;
        this.internal = FluidRegistry.getFluid(ident);
    }

    public static Fluid getFluid(String type) {
        if (!registryCache.containsKey(type)) {
            registryCache.put(type, new Fluid(type));
        }
        return registryCache.get(type);
    }

    public static Fluid getFluid(net.minecraftforge.fluids.Fluid fluid) {
        return getFluid(FluidRegistry.getFluidName(fluid));
    }

    public int getDensity() {
        return internal.getDensity();
    }
}
