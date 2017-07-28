package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.LocomotiveSteamDefinition;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LocomotiveSteam extends Locomotive implements IFluidHandler {
	public LocomotiveSteam(World world) {
		this(world, null);
	}

	public LocomotiveSteam(World world, String defID) {
		super(world, defID);
	}

	protected LocomotiveSteamDefinition getDefinition() {
		return (LocomotiveSteamDefinition) DefinitionManager.getDefinition(defID);
	}

	@Override
	public int getInventorySize() {
		return 3 + 3 + 3 + 1 + 1;
	}

	public int[] getLocomotiveInventorySizes() {
		return new int[] { 3, 3, 3 };
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			return;
		}

		if (rand.nextInt(100) == 0 && getTankCapacity() > 0 && isFuelled()) {
			drain(getWaterConsumption() / 5, true);
		}
	}

	@Override
	public int[] getContainerInputSlots() {
		return new int[] { 1 };
	}

	@Override
	public int[] getContainertOutputSlots() {
		return ArrayUtils.removeElement(super.getContainertOutputSlots(), 0);
	}

	/*
	 * TODO
	 * 
	 * @Override protected Integer customHeatHandler(Integer overheatLevel) {
	 * int waterLevel = this.getLiquidAmount(); int maxWaterLevel =
	 * getCarTankCapacity();
	 * 
	 * if (waterLevel < 1 && getFuel() > 10) { overheatLevel += 5; } if
	 * (waterLevel > maxWaterLevel / 2 && overheatLevel > 50 &&
	 * !getState().equals("broken")) { overheatLevel -= 1; } return
	 * overheatLevel; }
	 */

	@Override
	protected void checkInvent() {
		super.checkInvent();

		if (cargoItems.getStackInSlot(0) != null) {
			int burnTime = ForgeEventFactory.getItemBurnTime(cargoItems.getStackInSlot(0));
			if (getFuel() + burnTime <= getMaxFuel()) {
				addFuel(burnTime);
				cargoItems.extractItem(0, 1, false);
			}
		}

		if (getFuel() <= 0) {
			motionX *= 0.88;
			motionZ *= 0.88;
		}

		Tender tender = null;

		// BUG: locomotives can drain from tenders in front of the locomotive
		if (this.getCoupled(CouplerType.FRONT) instanceof Tender) {
			tender = (Tender) getCoupled(CouplerType.FRONT);
		}
		if (this.getCoupled(CouplerType.BACK) instanceof Tender) {
			tender = (Tender) getCoupled(CouplerType.BACK);
		}

		if (tender == null) {
			return;
		}

		// Only drain 10mb at a time from the tender
		int desiredDrain = 10;
		if (getTankCapacity() - getLiquidAmount() >= 10) {
			FluidUtil.tryFluidTransfer(this, tender, desiredDrain, true);
		}

		/*
		 * TODO for (int tenderID = 0; tenderID < tender.getInventorySize();
		 * tenderID++) { ItemStack tenderItem = tender.cargoItems[tenderID];
		 * ItemStack locoItem = cargoItems[0]; if
		 * (TraincraftUtil.steamFuelBurnTime(tenderItem) > 0) { if (locoItem ==
		 * null) { tender.decrStackSize(tenderID, 1);
		 * 
		 * cargoItems[0] = tenderItem.copy(); cargoItems[0].stackSize = 1;
		 * break; } else if (locoItem.isItemEqual(tenderItem) &&
		 * locoItem.getMaxStackSize() > locoItem.stackSize) {
		 * tender.decrStackSize(tenderID, 1);
		 * 
		 * cargoItems[0].stackSize++; break; } } }
		 */
	}

	/** Used for the gui */
	@Override
	public int getFuelDiv(int i) {
		return (int) ((this.getFuel() * i) / getMaxFuel());
	}

	public int getWaterConsumption() {
		return this.getDefinition().getWaterConsumption();
	}

	@Override
	public int getTankCapacity() {
		return this.getDefinition().getTankCapacity();
	}

	@Override
	public double getMaxFuel() {
		return this.getDefinition().getFuelCapacity();
	}

	@Override
	public List<Fluid> getFluidFilter() {
		List<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.WATER);
		return filter;
	}
}