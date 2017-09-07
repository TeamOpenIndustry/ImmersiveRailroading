package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
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

	public LocomotiveSteamDefinition getDefinition() {
		return (LocomotiveSteamDefinition) DefinitionManager.getDefinition(defID);
	}

	@Override
	public GuiTypes guiType() {
		return GuiTypes.STEAM_LOCOMOTIVE;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			return;
		}

		if (rand.nextInt(100) == 0 && getTankCapacity() > 0 && getFuel() > 0) {
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
			if (burnTime > 0 && getFuel() + burnTime <= this.getDefinition().getFuelCapacity()) {
				addFuel(burnTime);
				// TODO shadow item which fades as it is used based on burn time
				cargoItems.extractItem(0, 1, false);
			}
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
		if (getTankCapacity() - getServerLiquidAmount() >= 10) {
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

	@Override
	public int getTankCapacity() {
		return this.getDefinition().getTankCapacity();
	}

	@Override
	public List<Fluid> getFluidFilter() {
		List<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.WATER);
		return filter;
	}
}