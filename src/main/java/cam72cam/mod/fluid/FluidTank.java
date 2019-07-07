package cam72cam.mod.fluid;

import cam72cam.mod.capability.ITank;
import cam72cam.mod.util.TagCompound;

public class FluidTank implements ITank {
    private final net.minecraftforge.fluids.FluidTank internal;

    public FluidTank(FluidStack fluidStack, int capacity) {
        if (fluidStack == null) {
            internal = new net.minecraftforge.fluids.FluidTank(capacity) {
                public void onContentsChanged() {
                    FluidTank.this.onChanged();
                }
            };
        } else {
            internal = new net.minecraftforge.fluids.FluidTank(fluidStack.internal, capacity) {
                public void onContentsChanged() {
                    FluidTank.this.onChanged();
                }
            };
        }
    }

    public void onChanged() {
        //NOP
    }

    @Override
    public FluidStack getContents() {
        return new FluidStack(internal.getFluid());
    }

    @Override
    public int getCapacity() {
        return internal.getCapacity();
    }

    @Override
    public boolean allows(Fluid fluid) {
        return internal.canFill();
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }
        return internal.fill(fluidStack.internal, !simulate);
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return null;
        }
        return new FluidStack(internal.drain(fluidStack.internal, !simulate));
    }

    public void setCapacity(int milliBuckets) {
        internal.setCapacity(milliBuckets);
    }

    public TagCompound write(TagCompound tag) {
        return new TagCompound(internal.writeToNBT(tag.internal));
    }

    public void read(TagCompound tag) {
        internal.readFromNBT(tag.internal);
    }

    public boolean tryFill(ITank inputTank, int max, boolean simulate) {
        int maxTransfer = this.fill(inputTank.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = this.fill(inputTank.getContents(), simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }

    public boolean tryDrain(ITank inputTank, int max, boolean simulate) {
        int maxTransfer = inputTank.fill(this.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = inputTank.fill(this.getContents(), simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }
}
