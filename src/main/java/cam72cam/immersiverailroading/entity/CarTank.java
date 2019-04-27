package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.registry.CarTankDefinition;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraftforge.fluids.Fluid;

public class CarTank extends FreightTank {

	public CarTank(net.minecraft.world.World world) {
		super(world);
	}

	@Override
	public CarTankDefinition getDefinition() {
		return super.getDefinition(CarTankDefinition.class);
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getTankCapaity(gauge);
	}

	@Override
	public List<Fluid> getFluidFilter() {
		return this.getDefinition().getFluidFilter();
	}
}
