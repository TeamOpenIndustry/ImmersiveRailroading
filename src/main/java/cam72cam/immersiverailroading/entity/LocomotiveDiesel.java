package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LocomotiveDiesel extends Locomotive implements IFluidHandler {

	public LocomotiveDiesel(World world) {
		this(world, null);
	}

	public LocomotiveDiesel(World world, String defID) {
		super(world, defID);
		//runSound.setDynamicPitch();
	}

	@Override
	protected void checkInvent() {
		super.checkInvent();
		if (getFuel() < getMaxFuel()) {
			int amount = (int) Math.min(10, getMaxFuel() - getFuel());
			FluidStack ableToDrain = drain(amount, true);
			if (ableToDrain != null) {
				addFuel(ableToDrain.amount);
			}
		}
	}

	public LocomotiveDieselDefinition getDefinition() {
		return (LocomotiveDieselDefinition) DefinitionManager.getDefinition(defID);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.DIESEL_LOCOMOTIVE;
	}

	public int getFuelDiv(int i) {
		return (int) ((this.getFuel() * i) / 1200);
	}
	
	@Override
	public List<Fluid> getFluidFilter() {
		ArrayList<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.getFluid("oil"));
		filter.add(FluidRegistry.getFluid("biofuel"));
		return filter;
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