package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarFreightDefinition;

public class CarFreight extends Freight {

	public CarFreight(net.minecraft.world.World world) {
		super(world);
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
