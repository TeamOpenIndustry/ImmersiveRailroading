package cam72cam.immersiverailroading.entity;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.RegisteredSteamLocomotive;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class SteamLocomotive extends Locomotive implements IFluidHandler {
	public SteamLocomotive(World world) {
		super(world, new Fluid[] { FluidRegistry.WATER });
		
		//Identifier from NBT
	}


	public SteamLocomotive(World world, String defID) {
		this(world);
		this.defID = defID;
	}
	
	
	private RegisteredSteamLocomotive getDefinition() {
		return (RegisteredSteamLocomotive) DefinitionManager.getDefinition(defID);
	}

	public double getMaxFuel() {
		return 2000.0;
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

	/* TODO
	@Override
	protected Integer customHeatHandler(Integer overheatLevel) {
		int waterLevel = this.getLiquidAmount();
		int maxWaterLevel = getCartTankCapacity();

		if (waterLevel < 1 && getFuel() > 10) {
			overheatLevel += 5;
		}
		if (waterLevel > maxWaterLevel / 2 && overheatLevel > 50 && !getState().equals("broken")) {
			overheatLevel -= 1;
		}
		return overheatLevel;
	}
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
		if (this.getLinkedCartFront() instanceof Tender) {
			tender = (Tender) getLinkedCartFront();
		}
		if (this.getLinkedCartBack() instanceof Tender) {
			tender = (Tender) getLinkedCartBack();
		}

		if (tender == null) {
			return;
		}

		// Only drain 10mb at a time from the tender
		int desiredDrain = 10;
		if (getTankCapacity() - getLiquidAmount() >= 10) {
			FluidUtil.tryFluidTransfer(this, tender, desiredDrain, true);
		}

		/* TODO
		for (int tenderID = 0; tenderID < tender.getInventorySize(); tenderID++) {
			ItemStack tenderItem = tender.cargoItems[tenderID];
			ItemStack locoItem = cargoItems[0];
			if (TraincraftUtil.steamFuelBurnTime(tenderItem) > 0) {
				if (locoItem == null) {
					tender.decrStackSize(tenderID, 1);

					cargoItems[0] = tenderItem.copy();
					cargoItems[0].stackSize = 1;
					break;
				} else if (locoItem.isItemEqual(tenderItem) && locoItem.getMaxStackSize() > locoItem.stackSize) {
					tender.decrStackSize(tenderID, 1);

					cargoItems[0].stackSize++;
					break;
				}
			}
		}*/
	}

	/** Used for the gui */
	@Override
	public int getFuelDiv(int i) {
		return (int) ((this.getFuel() * i) / getMaxFuel());
	}


	@Override
	public void render(double x, double y, double z, float entityYaw, float partialTicks) {
		if (this.getDefinition() != null) {
			this.getDefinition().render(this, x, y, z, entityYaw, partialTicks);
		} else {
			this.getEntityWorld().removeEntity(this);
		}
	}
	
	public int getWaterConsumption() {
		//return this.getDefinition().getWaterConsumption();
		return 0;
	}

	@Override
	public void updatePassenger(Entity passenger) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int getDefaultFuelConsumption() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int getDefaultPower() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getDefaultAccel() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getDefaultBrake() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public Speed getMaxSpeed() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected float frontBogeyOffset() {
		return 3;
	}


	@Override
	protected float rearBogeyOffset() {
		return -1;
	}


	@Override
	public int getTankCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}
}