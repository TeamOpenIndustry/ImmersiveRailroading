package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.gui.ISyncableSlots;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public abstract class FreightTank extends Freight implements IFluidHandler {
	private static final DataParameter<Integer> FLUID_AMOUNT = EntityDataManager.createKey(FreightTank.class, DataSerializers.VARINT);
	private static final DataParameter<String> FLUID_TYPE = EntityDataManager.createKey(FreightTank.class, DataSerializers.STRING);
	private FluidTank theTank;

	public FreightTank(World world, String defID) {
		super(world, defID);
		
		dataManager.register(FLUID_AMOUNT, 0);
		dataManager.register(FLUID_TYPE, "EMPTY");
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
		return this.dataManager.get(FLUID_AMOUNT);
	}
	
	public Fluid getLiquid() {
		String type = this.dataManager.get(FLUID_TYPE);
		if (type.equals("EMPTY")) {
			return null;
		}
		return FluidRegistry.getFluid(type);
	}
	
	
	/*
	 * 
	 * Server functions
	 * 
	 */

	protected void onTankContentsChanged() {
		this.dataManager.set(FLUID_AMOUNT, theTank.getFluidAmount());
		if (theTank.getFluid() == null) {
			this.dataManager.set(FLUID_TYPE, "EMPTY");
		} else {
			this.dataManager.set(FLUID_TYPE, FluidRegistry.getFluidName(theTank.getFluid()));
		}
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
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		// TODO right click with tanks/buckets
		if(!world.isRemote) {
			System.out.println(theTank.getFluidAmount());
		}
		return super.processInitialInteract(player, hand);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setTag("tank", this.theTank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		this.theTank.readFromNBT(nbttagcompound.getCompoundTag("tank"));
		onTankContentsChanged();
	}
	
	@Override
	public void rollingStockInit() {
		super.rollingStockInit();
		theTank = new FluidTank(null, 0) {
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return canFill() && (getFluidFilter() == null || getFluidFilter().contains(fluid.getFluid()));
			}
			
			@Override
			public void onContentsChanged() {
				if (!world.isRemote) {
					FreightTank.this.onTankContentsChanged();
				}
			}
		};

		theTank.setCapacity(this.getTankCapacity().MilliBuckets());
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		checkInvent();
	}

	protected void checkInvent() {

		if (world.isRemote) {
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

	/*
	 * 
	 * IFluidHandler Overrides
	 * 
	 */
	
	@Override
	public double getWeight() {
		// LiquidWeight 
		double fLoad = Config.liquidWeight * (this.getDataManager().get(FLUID_AMOUNT) / 1000f);
		fLoad = fLoad + super.getWeight();
		return fLoad;
	}
	
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

	private List<ISyncableSlots> listners = new ArrayList<ISyncableSlots>();
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
