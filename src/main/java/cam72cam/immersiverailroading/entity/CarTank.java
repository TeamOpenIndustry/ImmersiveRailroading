package cam72cam.immersiverailroading.entity;

import java.util.List;

import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class CarTank extends FreightTank {
	public CarTank(World world) {
		this(world, null);
	}
	
	public CarTank(World world, String defID) {
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
