package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarFreightDefinition;
import cam72cam.mod.entity.ModdedEntity;

public class CarFreight extends Freight {

	public CarFreight(ModdedEntity entity) {
		super(entity);
	}
	
	@Override
	public CarFreightDefinition getDefinition() {
		return super.getDefinition(CarFreightDefinition.class);
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize(gauge);
	}
	
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth(gauge);
	}

	//TODO filter inventory
}
