package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.RegisteredDieselLocomotive;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class DieselLocomotive extends Locomotive implements IFluidHandler {

	public DieselLocomotive(World world) {
		this(world, null);
	}

	public DieselLocomotive(World world, String defID) {
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

	protected RegisteredDieselLocomotive getDefinition() {
		return (RegisteredDieselLocomotive) DefinitionManager.getDefinition(defID);
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