package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidTank;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.util.TagCompound;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.List;

public abstract class FreightTank extends Freight {
	private static final String FLUID_AMOUNT = "FLUID_AMOUNT";
	private static final String FLUID_TYPE = "FLUID_TYPE";

	public final FluidTank theTank = new FluidTank(null, 0) {
		@Override
		public boolean allows(Fluid fluid) {
			return (getFluidFilter() == null || getFluidFilter().contains(fluid));
		}
		
		@Override
		public void onChanged() {
			if (getWorld().isServer) {
				FreightTank.this.onTankContentsChanged();
			}
		}
	};

	public FreightTank() {
		sync.setInteger(FLUID_AMOUNT, 0);
		sync.setString(FLUID_TYPE, "EMPTY");
	}

	/*
	 * 
	 * Specifications
	 */
	public abstract FluidQuantity getTankCapacity();

	/**
	 * null == all
	 * [] == none
	 */
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
		return sync.getInteger(FLUID_AMOUNT);
	}
	
	public Fluid getLiquid() {
		String type = sync.getString(FLUID_TYPE);
		if (type.equals("EMPTY")) {
			return null;
		}
		return Fluid.getFluid(type);
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
		onTankContentsChanged();
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		this.theTank.drain(this.theTank.getContents(), true);
		this.theTank.setCapacity(0);
		onTankContentsChanged();
	}

	protected void onTankContentsChanged() {
		if (getWorld().isClient) {
			return;
		}
		
		sync.setInteger(FLUID_AMOUNT, theTank.getContents().getAmount());
		if (theTank.getContents().getFluid() == null) {
			sync.setString(FLUID_TYPE, "EMPTY");
		} else {
			sync.setString(FLUID_TYPE, theTank.getContents().getFluid().ident);
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
	public GuiRegistry.GUIType guiType() {
		return GuiTypes.TANK;
	}

	@Override
	public void save(TagCompound data) {
		super.save(data);
		data.set("tank", this.theTank.write(new TagCompound()));
	}

	@Override
	public void load(TagCompound data) {
		super.load(data);
		this.theTank.read(data.get("tank"));
		onTankContentsChanged();
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
						success = theTank.tryDrain(inputTank, theTank.getCapacity(), true);
					} else {
						success = theTank.tryFill(inputTank, theTank.getCapacity(), true);
					}

					if (success) {
						// We were able to drain into the container

						// Can we move it to an output slot?
						ItemStack out = inputCopy[0].copy();
						for (Integer slot : this.getContainertOutputSlots()) {
							if (this.cargoItems.insert(slot, out, true).getCount() == 0) {
								// Move Liquid
								if (doFill) {
									theTank.tryDrain(inputTank, theTank.getCapacity(), false);
								} else {
									theTank.tryFill(inputTank, theTank.getCapacity(), false);
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
		if (this.getLiquidAmount() > 0) {
			fLoad += this.getLiquidAmount() * this.getLiquid().getDensity() / Fluid.BUCKET_VOLUME;
		}
		return fLoad;
	}
	
	public int getPercentLiquidFull() {
		return this.getLiquidAmount() * 100 / this.getTankCapacity().MilliBuckets();
	}
}
