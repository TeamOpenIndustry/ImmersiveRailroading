package cam72cam.immersiverailroading.entity;

import net.minecraft.world.World;

public class CartPassenger extends EntityMoveableRollingStock {
	public CartPassenger(World world) {
		this(world, null);
	}
	
	public CartPassenger(World world, String defID) {
		super(world, defID);
	}
}
