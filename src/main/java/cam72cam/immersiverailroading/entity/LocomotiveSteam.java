package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LocomotiveSteam extends Locomotive implements IFluidHandler {
	public LocomotiveSteam(World world) {
		this(world, null);
	}

	public LocomotiveSteam(World world, String defID) {
		super(world, defID);
	}

	public LocomotiveSteamDefinition getDefinition() {
		return (LocomotiveSteamDefinition) DefinitionManager.getDefinition(defID);
	}

	@Override
	public GuiTypes guiType() {
		return GuiTypes.STEAM_LOCOMOTIVE;
	}
	
	@Override
	protected int getAvailableHP() {
		return this.getDefinition().getHorsePower();
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			return;
		}
		
		Tender tender = null;
		if (this.getCoupled(CouplerType.BACK) instanceof Tender) {
			tender = (Tender) getCoupled(CouplerType.BACK);
		}

		if (tender == null) {
			return;
		}

		// Only drain 10mb at a time from the tender
		int desiredDrain = 10;
		if (getTankCapacity().MilliBuckets() - getServerLiquidAmount() >= 10) {
			FluidUtil.tryFluidTransfer(this, tender, desiredDrain, true);
		}

		if (rand.nextInt(100) == 0 && getTankCapacity().MilliBuckets() > 0) {
			//TODO heat check
			drain(this.getDefinition().getWaterConsumption(), true);
		}
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize() + 2;
	}
	
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth();
	}
	
	@Override
	protected int[] getContainerInputSlots() {
		return new int[] { getInventorySize()-2 };
	}
	@Override
	protected int[] getContainertOutputSlots() {
		return new int[] { getInventorySize()-1 };
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getTankCapacity();
	}

	@Override
	public List<Fluid> getFluidFilter() {
		List<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.WATER);
		return filter;
	}
}