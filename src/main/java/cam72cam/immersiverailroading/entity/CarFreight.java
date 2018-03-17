package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarFreightDefinition;
import net.minecraft.world.World;

public class CarFreight extends Freight {
	public CarFreight(World world) {
		this(world, null);
	}
	
	public CarFreight(World world, String defID) {
		super(world, defID);
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
