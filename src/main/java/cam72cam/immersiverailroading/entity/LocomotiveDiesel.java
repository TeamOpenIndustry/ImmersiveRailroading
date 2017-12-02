package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import blusunrize.immersiveengineering.api.energy.DieselHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class LocomotiveDiesel extends Locomotive {

	public LocomotiveDiesel(World world) {
		this(world, null);
	}

	public LocomotiveDiesel(World world, String defID) {
		super(world, defID);
		//runSound.setDynamicPitch();
	}

	public LocomotiveDieselDefinition getDefinition() {
		return super.getDefinition(LocomotiveDieselDefinition.class);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.DIESEL_LOCOMOTIVE;
	}
	
	
	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Entity source, KeyTypes key) {
		super.handleKeyPress(source, key);
		
		this.mapTrain(this, true, false, this::setThrottleMap);
	}
	
	private void setThrottleMap(EntityRollingStock stock, boolean direction) {
		if (stock instanceof LocomotiveDiesel) {
			((LocomotiveDiesel) stock).setThrottle(this.getThrottle() * (direction ? 1 : -1));
			((LocomotiveDiesel) stock).setAirBrake(this.getAirBrake());
		}
	}
	
	@Override
	protected int getAvailableHP() {
		return this.getLiquidAmount() > 0 ? this.getDefinition().getHorsePower(gauge) : 0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (this.getLiquidAmount() > 0 && getThrottle() != 0) {
			int burnTime = DieselHandler.getBurnTime(this.getLiquid());
			if (burnTime == 0) {
				burnTime = 200; //Default to 200 for unregistered liquids
			}
			burnTime *= getDefinition().getFuelEfficiency()/100f;
			burnTime /= Math.abs(getThrottle())*10;
			burnTime *= 1/gauge.scale();
			if (this.ticksExisted % burnTime == 0) {
				theTank.drain(1, true);
			}
		}
	}
	
	@Override
	public List<Fluid> getFluidFilter() {
		ArrayList<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.getFluid("oil"));
		filter.add(FluidRegistry.getFluid("fuel"));
		filter.add(FluidRegistry.getFluid("diesel"));
		filter.add(FluidRegistry.getFluid("ethanol"));
		filter.add(FluidRegistry.getFluid("biofuel"));
		filter.add(FluidRegistry.getFluid("biodiesel"));
		return filter;
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getFuelCapacity(gauge);
	}
}