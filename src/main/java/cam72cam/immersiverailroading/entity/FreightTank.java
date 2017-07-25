package cam72cam.immersiverailroading.entity;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public abstract class FreightTank extends Freight implements IFluidHandler {
	private FluidTank theTank;

	public FreightTank(World world, String defID) {
		super(world, defID);
	}

	/*
	 * 
	 * Specifications
	 */
	public abstract int getTankCapacity();

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

	/*
	 * 
	 * Functions for Models and GUI
	 */
	public int getLiquidAmount() {
		return theTank.getFluidAmount();
	}

	public FluidStack getLiquid() {
		return theTank.getFluid();
	}

	@SideOnly(Side.CLIENT)
	public int getCartTankCapacity() {
		return theTank.getCapacity();
	}

	/*
	 * 
	 * Freight Overrides
	 */

	@Override
	protected GuiTypes guiType() {
		return GuiTypes.TANK;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		System.out.println(this.theTank);
		this.theTank.writeToNBT(nbttagcompound.getCompoundTag("tank"));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		this.theTank.readFromNBT(nbttagcompound.getCompoundTag("tank"));
	}
	
	@Override
	protected void rollingStockInit() {
		super.rollingStockInit();
		theTank = new FluidTank(null, 0) {
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return canFill() && (getFluidFilter() == null || getFluidFilter().contains(fluid.getFluid()));
			}
		};

		theTank.setCapacity(this.getTankCapacity());
	}

	protected void onInventoryChanged() {
		super.onInventoryChanged();
		if (!world.isRemote) {
			checkInvent();
		}
	}

	/**
	 * Handle mass depending on liquid amount
	 */
	@Override
	protected void handleMass() {
		int mass = 0;
		if (theTank.getFluid() != null) {
			// 1 bucket = 1 kilo
			mass += theTank.getFluid().amount / 10000;
		}
		this.getDataManager().set(CARGO_MASS, mass);
	}

	protected void checkInvent() {

		for (int inputSlot : getContainerInputSlots()) {
			ItemStack input = cargoItems.getStackInSlot(inputSlot);

			if (input == null) {
				continue;
			}

			ItemStack inputCopy = ItemHandlerHelper.copyStackWithSize(input, 1);
			IFluidHandlerItem containerFluidHandler = FluidUtil.getFluidHandler(inputCopy);

			if (containerFluidHandler == null) {
				continue;
			}

			// This is kind of funky but it works
			while (input.getCount() > 0) {
				// First try to drain the container, if we can't do that we try
				// to fill it

				boolean didAction = false;

				for (Boolean doFill : new Boolean[] { false, true }) {

					FluidActionResult inputAttempt;

					if (doFill) {
						inputAttempt = FluidUtil.tryFillContainer(inputCopy, theTank, Integer.MAX_VALUE, null, false);
					} else {
						inputAttempt = FluidUtil.tryEmptyContainer(inputCopy, theTank, Integer.MAX_VALUE, null, false);
					}

					if (inputAttempt.isSuccess()) {
						// We were able to drain into the container

						// Can we move it to an output slot?
						ItemStack out = inputAttempt.getResult();
						for (Integer slot : this.getContainertOutputSlots()) {
							if (this.cargoItems.insertItem(slot, out, false).getCount() == 1) {
								out.setCount(0);
								break;
							}
						}

						// We moved it to the output
						if (out.getCount() == 1) {
							input.setCount(input.getCount() - 1);
							cargoItems.setStackInSlot(inputSlot, input);

							if (doFill) {
								FluidUtil.tryFillContainer(inputCopy, theTank, Integer.MAX_VALUE, null, true);
							} else {
								FluidUtil.tryEmptyContainer(inputCopy, theTank, Integer.MAX_VALUE, null, true);
							}
							didAction = true;
							break;
						}
					}
				}

				if (!didAction) {
					// Unable to move any stuff around
					break;
				}
			}
		}

		// Update mass since we may have changed the tank volume
		// Do we need this?
		handleMass();
	}

	/*
	 * 
	 * IFluidHandler Overrides
	 * 
	 */

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return theTank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return theTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return theTank.drain(resource, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return theTank.drain(maxDrain, doDrain);
	}
}
