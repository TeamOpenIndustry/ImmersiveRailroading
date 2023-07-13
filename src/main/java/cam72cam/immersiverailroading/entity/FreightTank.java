package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidTank;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.List;

public abstract class FreightTank extends Freight {
	@TagField("tank")
	public FluidTank theTank = new FluidTank(null, 0);

	@TagSync
	@TagField("FLUID_AMOUNT")
	private int fluidAmount = 0;

	@TagSync
	@TagField(value = "FLUID_TYPE", mapper = StrictTagMapper.class)
	private String fluidType = null;

	/*
	 * 
	 * Specifications
	 */
	public abstract FluidQuantity getTankCapacity();

	@Nullable
	public abstract List<Fluid> getFluidFilter();

	protected int[] getContainerInputSlots() {
		return new int[] { 0 };
	}

	protected int[] getContainertOutputSlots() {
		int[] result = new int[getInventorySize()];
		for (int i = 0; i < getInventorySize(); i++) {
			result[i] = i;
		}

		for (int i : getContainerInputSlots()) {
			result = ArrayUtils.removeElement(result, i);
		}

		return result;
	}

	/*
	 * 
	 * Freight Specification Overrides
	 */
	@Override
	public int getInventorySize() {
		return 2;
	}
	
	public int getLiquidAmount() {
		return fluidAmount;
	}
	
	public Fluid getLiquid() {
		if (fluidType == null) {
			return null;
		}
		return Fluid.getFluid(fluidType);
	}
	
	@Override
	protected void initContainerFilter() {
		cargoItems.filter.clear();
		cargoItems.filter.put(0, SlotFilter.FLUID_CONTAINER);
		cargoItems.filter.put(1, SlotFilter.FLUID_CONTAINER);
		cargoItems.defaultFilter = SlotFilter.NONE;
	}
	
	
	/*
	 * 
	 * Server functions
	 * 
	 */
	
	@Override
	public void onAssemble() {
		super.onAssemble();
		this.theTank.setCapacity(this.getTankCapacity().MilliBuckets());
		this.theTank.setFilter(this::getFluidFilter);
		this.theTank.onChanged(this::onTankContentsChanged);
		onTankContentsChanged();
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		this.theTank.setCapacity(0);
		onTankContentsChanged();
	}

	protected void onTankContentsChanged() {
		if (getWorld().isClient) {
			return;
		}
		
		fluidAmount =  theTank.getContents().getAmount();
		if (theTank.getContents().getFluid() == null) {
			fluidType = null;
		} else {
			fluidType = theTank.getContents().getFluid().ident;
		}
	}
	
	public int getServerLiquidAmount() {
		return theTank.getContents().getAmount();
	}

	/*
	 * 
	 * Freight Overrides
	 */

	@Override
	public boolean openGui(Player player) {
		if (player.hasPermission(Permissions.FREIGHT_INVENTORY)) {
			GuiTypes.TANK.open(player, this);
		}
		return true;
	}

	@Override
	public void onTick() {
		super.onTick();
		checkInvent();
	}

	protected void checkInvent() {

		if (getWorld().isClient) {
			return;
		}
		
		if (!this.isBuilt()) {
			return;
		}
		
		if (cargoItems.getSlotCount() == 0) {
			return;
		}

		for (int inputSlot : getContainerInputSlots()) {
			ItemStack input = cargoItems.get(inputSlot);

			final ItemStack[] inputCopy = {input.copy()};
			inputCopy[0].setCount(1);
			ITank inputTank = ITank.getTank(inputCopy[0], (ItemStack stack) -> inputCopy[0] = stack);

			if (inputTank == null) {
				continue;
			}

			// This is kind of funky but it works
			// WILL BE CALLED RECUSIVELY from onInventoryChanged
			if (input.getCount() > 0) {
				// First try to drain the container, if we can't do that we try
				// to fill it

				for (Boolean doFill : new Boolean[] { false, true }) {
					boolean success;
					if (doFill) {
						success = theTank.drain(inputTank, theTank.getCapacity(), true) > 0;
					} else {
						success = theTank.fill(inputTank, theTank.getCapacity(), true) > 0;
					}

					if (success) {
						// We were able to drain into the container

						// Can we move it to an output slot?
						ItemStack out = inputCopy[0].copy();
						for (Integer slot : this.getContainertOutputSlots()) {
							if (this.cargoItems.insert(slot, out, true).getCount() == 0) {
								// Move Liquid
								if (doFill) {
									theTank.drain(inputTank, theTank.getCapacity(), false);
								} else {
									theTank.fill(inputTank, theTank.getCapacity(), false);
								}
								if (!ConfigDebug.debugInfiniteLiquids) {
									// Decrease input
									cargoItems.extract(inputSlot, 1, false);
									
									// Increase output
									this.cargoItems.insert(slot, out, false);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	/*
	 * 
	 * ITank Overrides
	 * 
	 */
	
	@Override
	public double getWeight() {
		double fLoad = super.getWeight();
		if (this.getLiquidAmount() > 0 && this.getLiquid() != null) {
			fLoad += this.getLiquidAmount() * this.getLiquid().getDensity() / Fluid.BUCKET_VOLUME;
		}
		return fLoad;
	}

	@Override
	public double getMaxWeight() {
		double waterDensity = 1000;
		return super.getMaxWeight() + getTankCapacity().Buckets() * waterDensity;
	}

	public int getPercentLiquidFull() {
		return this.getLiquidAmount() * 100 / this.getTankCapacity().MilliBuckets();
	}
}
