package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CableCarDefinition;
import cam72cam.mod.math.Vec3d;

public class CableCar extends LocomotiveUnfueled {

	public CableCar() {
		super();
		this.setIgnoreSlope(true);
	}

	@Override
	public int getInventoryWidth() {
		return 2;
	}

	@Override
	public CableCarDefinition getDefinition() {
		return super.getDefinition(CableCarDefinition.class);
	}

	@Override
	protected int getAvailableHP() {
		return this.getDefinition().getHorsePower(gauge);
	}
}