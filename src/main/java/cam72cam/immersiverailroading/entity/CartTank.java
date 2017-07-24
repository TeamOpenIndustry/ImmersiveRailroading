package cam72cam.immersiverailroading.entity;

import java.util.List;

import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class CartTank extends FreightTank {
	public CartTank(World world) {
		this(world, null);
	}
	
	public CartTank(World world, String defID) {
		super(world, defID);
	}

	@Override
	public int getInventorySize() {
		return 0;
	}

	@Override
	public int getTankCapacity() {
		return 0;
	}

	@Override
	public List<Fluid> getFluidFilter() {
		return null;
	}
}
