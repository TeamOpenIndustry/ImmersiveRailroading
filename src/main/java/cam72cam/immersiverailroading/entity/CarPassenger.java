package cam72cam.immersiverailroading.entity;

import net.minecraft.world.World;

public class CarPassenger extends EntityCoupleableRollingStock {
	public CarPassenger(World world) {
		this(world, null);
	}
	
	public CarPassenger(World world, String defID) {
		super(world, defID);
	}
}
