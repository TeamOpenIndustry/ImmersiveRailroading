package cam72cam.immersiverailroading.entity;

import net.minecraft.world.World;

public class CarFreight extends Freight {
	public CarFreight(World world) {
		this(world, null);
	}
	
	public CarFreight(World world, String defID) {
		super(world, defID);
	}

	@Override
	public int getInventorySize() {
		return 0;
	}
}
