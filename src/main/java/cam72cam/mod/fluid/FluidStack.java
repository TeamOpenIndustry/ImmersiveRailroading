package cam72cam.mod.fluid;

public class FluidStack {
    public final net.minecraftforge.fluids.FluidStack internal;

    public FluidStack(net.minecraftforge.fluids.FluidStack stack) {
        this.internal = stack;
    }

    public FluidStack(Fluid fluid, int amount) {
        this.internal = new net.minecraftforge.fluids.FluidStack(fluid.internal, amount);
    }

    public Fluid getFluid() {
        if (internal == null) {
            return null;
        }
        return Fluid.getFluid(internal.getFluid().getUnlocalizedName());
    }

    public int getAmount() {
        if (internal == null) {
            return 0;
        }
        return internal.amount;
    }
}
