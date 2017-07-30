package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.entity.registry.CarTankDefinition;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.gui.ISyncableSlots;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class CarTank extends FreightTank {
	private List<ISyncableSlots> listners = new ArrayList<ISyncableSlots>();

	public CarTank(World world) {
		this(world, null);
	}
	
	public CarTank(World world, String defID) {
		super(world, defID);
	}
	
	protected CarTankDefinition getDefinition() {
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
	
	@Override
	protected void onInventoryChanged() {
		super.onInventoryChanged();
		if (!world.isRemote) {
			for(ISyncableSlots container : listners) {
				container.syncSlots();;
			}
		}
	}

	public void addListener(ISyncableSlots tankContainer) {
		this.listners.add(tankContainer);
	}
}
