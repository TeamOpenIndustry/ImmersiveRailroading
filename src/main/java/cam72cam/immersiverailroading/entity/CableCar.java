package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.CableCarDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.GuiRegistry;

import java.util.List;

public class CableCar extends LocomotiveUnfueled {

	private float soundThrottle;

	public CableCar() {
		super();
	}

	@Override
	public int getInventoryWidth() {
		return 2;
	}

	@Override
	public CableCarDefinition getDefinition() {
		return super.getDefinition(CableCarDefinition.class);
	}

	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Player source, KeyTypes key) {
		super.handleKeyPress(source, key);
	}
	
	private void realSetThrottle(float newThrottle) {
		super.setThrottle(newThrottle);
	}
	private void realAirBrake(float newAirBrake) {
		super.setAirBrake(newAirBrake);
	}
	
	@Override
	public void setThrottle(float newThrottle) {
		realSetThrottle(newThrottle);
	}
	
	@Override
	public void setAirBrake(float newAirBrake) {
		realAirBrake(newAirBrake);
	}
	
	@Override
	protected int getAvailableHP() {
		return this.getDefinition().getHorsePower(gauge);
	}

	@Override
	public void onTick() {
		super.onTick();
		
		if (getWorld().isClient) {
			float absThrottle = Math.abs(this.getThrottle());
			if (this.soundThrottle > absThrottle) {
				this.soundThrottle -= Math.min(0.01f, this.soundThrottle - absThrottle);
			} else if (this.soundThrottle < absThrottle) {
				this.soundThrottle += Math.min(0.01f, absThrottle - this.soundThrottle);
			}
			return;
		}
	}
	
	@Override
	public List<Fluid> getFluidFilter() {
		return BurnUtil.burnableFluids();
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return FluidQuantity.ZERO;
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
	}

	public float getSoundThrottle() {
		return soundThrottle;
	}
}