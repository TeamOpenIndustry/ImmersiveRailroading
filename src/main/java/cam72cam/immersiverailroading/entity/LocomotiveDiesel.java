package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.LocomotiveDieselDefinition;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LocomotiveDiesel extends Locomotive implements IFluidHandler {

	public LocomotiveDiesel(World world) {
		this(world, null);
	}

	public LocomotiveDiesel(World world, String defID) {
		super(world, defID, FluidRegistry.getFluid("oil"), FluidRegistry.getFluid("biofuel"));
		//runSound.setDynamicPitch();
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

	protected LocomotiveDieselDefinition getDefinition() {
		return (LocomotiveDieselDefinition) DefinitionManager.getDefinition(defID);
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

	@Override
	public int getTankCapacity() {
		return this.getDefinition().getFuelCapacity();
	}

	@Override
	public double getMaxFuel() {
		return this.getDefinition().getFuelCapacity();
	}
}