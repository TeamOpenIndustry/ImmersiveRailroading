package cam72cam.immersiverailroading.entity;

import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public abstract class DieselLocomotive extends Locomotive implements IFluidHandler {

	public DieselLocomotive(World world) {
		super(world, new Fluid[] { FluidRegistry.getFluid("oil"), FluidRegistry.getFluid("biofuel") });
		runSound.setDynamicPitch();
	}
	
	@Override
	protected void checkInvent() {
		super.checkInvent();
		if (getFuel() < getMaxFuel()) {
			int amount = (int) Math.min(10, getMaxFuel() - getFuel());
			FluidStack ableToDrain = drain(amount, true);
			addFuel(ableToDrain.amount);
		}
	}
	
	public double getMaxFuel() {
		return 2000.0;
	}

	public int getFuelDiv(int i) {
		return (int) ((this.getFuel() * i) / 1200);
	}

	public int[] getLocomotiveInventorySizes() {
		return new int[] { 3, 3, 3 };
	}

	@Override
	public int getInventorySize() {
		return 3 + 3 + 3 + 1;
	}
}