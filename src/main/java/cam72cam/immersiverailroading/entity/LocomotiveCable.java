package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.CableLocomotiveDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.math.Vec3d;

import java.util.List;

public class LocomotiveCable extends LocomotiveUnfueled {

	private float soundThrottle;

	public LocomotiveCable() {
		super();
		this.setIgnoreSlope(true);
	}

	@Override
	public int getInventoryWidth() {
		return 2;
	}

	@Override
	public CableLocomotiveDefinition getDefinition() {
		return super.getDefinition(CableLocomotiveDefinition.class);
	}

	@Override
	protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
		super.createExplosion(pos, size, damageTerrain);
	}

	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Player source, KeyTypes key) {
		super.handleKeyPress(source, key);
	}

	@Override
	protected void realSetThrottle(float newThrottle) {
		super.setThrottle(newThrottle);
	}

	@Override
	protected void realAirBrake(float newAirBrake) {
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
}