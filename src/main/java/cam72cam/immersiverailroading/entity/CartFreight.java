package cam72cam.immersiverailroading.entity;

import net.minecraft.world.World;

public class CartFreight extends Freight {
	public CartFreight(World world) {
		this(world, null);
	}
	
	public CartFreight(World world, String defID) {
		super(world, defID);
	}

	@Override
	public int getInventorySize() {
		return 0;
	}
}
