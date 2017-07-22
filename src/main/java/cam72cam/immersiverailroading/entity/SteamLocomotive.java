package cam72cam.immersiverailroading.entity;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.RegisteredSteamLocomotive;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class SteamLocomotive extends Locomotive implements IFluidHandler {
	public SteamLocomotive(World world) {
		this(world, null);
	}
	public SteamLocomotive(World world, String defID) {
		super(world, defID, FluidRegistry.WATER);
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
		return this.getDefinition().getWaterConsumption();
	}

	@Override
	public void updatePassenger(Entity passenger) {
		Vec3d offset = this.getDefinition().getPlayerOffset();
		offset = offset.add(new Vec3d(this.getPosition()));
		passenger.setPosition(offset.x, offset.y, offset.z);
	}


	@Override
	public int getDefaultFuelConsumption() {
		return this.getDefinition().getFuelConsumption();
	}


	@Override
	public int getDefaultPower() {
		return this.getDefinition().getPower();
	}


	@Override
	public double getDefaultAccel() {
		return this.getDefinition().getAccel();
	}


	@Override
	public double getDefaultBrake() {
		return this.getDefinition().getBrake();
	}


	@Override
	public Speed getMaxSpeed() {
		return this.getDefinition().getMaxSpeed();
	}


	@Override
	protected float frontBogeyOffset() {
		return this.getDefinition().getBogeyFront();
	}


	@Override
	protected float rearBogeyOffset() {
		return this.getDefinition().getBogeyRear();
	}


	@Override
	public int getTankCapacity() {
		return this.getDefinition().getTankCapacity();
	}
}