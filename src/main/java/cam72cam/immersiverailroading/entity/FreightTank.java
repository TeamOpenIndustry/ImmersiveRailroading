package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.util.TagCompound;
import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.gui.ISyncableSlots;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public abstract class FreightTank extends Freight {
	private static final String FLUID_AMOUNT = "FLUID_AMOUNT";
	private static final String FLUID_TYPE = "FLUID_TYPE";

	protected final FluidTank theTank = new FluidTank(null, 0) {
		@Override
		public boolean canFillFluidType(FluidStack fluid) {
			return canFill() && (getFluidFilter() == null || getFluidFilter().contains(fluid.getFluid()));
		}
		
		@Override
		public void onContentsChanged() {
			if (getWorld().isServer) {
				FreightTank.this.onTankContentsChanged();
			}
		}
	};;

	public FreightTank(ModdedEntity entity) {
		super(entity);
		
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
		return FluidRegistry.getFluid(type);
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
		this.theTank.drain(this.theTank.getFluidAmount(), true);
		this.theTank.setCapacity(0);
		onTankContentsChanged();
	}

	protected void onTankContentsChanged() {
		if (getWorld().isClient) {
			return;
		}
		
		sync.setInteger(FLUID_AMOUNT, theTank.getFluidAmount());
		if (theTank.getFluid() == null) {
			sync.setString(FLUID_TYPE, "EMPTY");
		} else {
			sync.setString(FLUID_TYPE, FluidRegistry.getFluidName(theTank.getFluid()));
		}
		sync.send();
	}
	
	public int getServerLiquidAmount() {
		return theTank.getFluidAmount();
	}

	/*
	 * 
	 * Freight Overrides
	 */

	@Override
	public GuiTypes guiType() {
		return GuiTypes.TANK;
	}

	@Override
	public void save(TagCompound data) {
		super.save(data);
		data.set("tank", new TagCompound(this.theTank.writeToNBT(new NBTTagCompound())));
	}

	@Override
	public void load(TagCompound data) {
		super.load(data);
		this.theTank.readFromNBT(data.get("tank").internal);
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
		
		if (cargoItems.getSlots() == 0) {
			return;
		}

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
			// WILL BE CALLED RECUSIVELY from onInventoryChanged
			if (input.getCount() > 0) {
				// First try to drain the container, if we can't do that we try
				// to fill it

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
							if (this.cargoItems.insertItem(slot, out, true).getCount() == 0) {
								// Move Liquid
								if (doFill) {
									FluidUtil.tryFillContainer(inputCopy, theTank, Integer.MAX_VALUE, null, true);
								} else {
									FluidUtil.tryEmptyContainer(inputCopy, theTank, Integer.MAX_VALUE, null, true);
								}
								if (!ConfigDebug.debugInfiniteLiquids) {
									// Decrease input
									cargoItems.extractItem(inputSlot, 1, false);
									
									// Increase output
									this.cargoItems.insertItem(slot, out, false);
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
	 * IFluidHandler Overrides
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

	private List<ISyncableSlots> listners = new ArrayList<ISyncableSlots>();
	@Override
	protected void onInventoryChanged() {
		super.onInventoryChanged();
		if (getWorld().isServer) {
			for(ISyncableSlots container : listners) {
				container.syncSlots();;
			}
		}
	}
	public void addListener(ISyncableSlots tankContainer) {
		this.listners.add(tankContainer);
	}
	
	@Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) theTank;
        }
        return super.getCapability(capability, facing);
    }
}
