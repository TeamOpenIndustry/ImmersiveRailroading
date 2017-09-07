package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.registry.CarTankDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class CarTank extends FreightTank {

	public CarTank(World world) {
		this(world, null);
	}
	
	public CarTank(World world, String defID) {
		super(world, defID);
	}
	
	public CarTankDefinition getDefinition() {
		return (CarTankDefinition) DefinitionManager.getDefinition(defID);
	}

	@Override
	public int getTankCapacity() {
		return this.getDefinition().getTankCapaity();
	}

	@Override
	public List<Fluid> getFluidFilter() {
		return this.getDefinition().getFluidFilter();
	}
}
