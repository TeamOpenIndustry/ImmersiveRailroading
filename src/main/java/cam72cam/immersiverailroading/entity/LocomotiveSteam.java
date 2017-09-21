package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.util.FluidQuantity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LocomotiveSteam extends Locomotive implements IFluidHandler {
	// PSI
	private static DataParameter<Float> BOILER_PRESSURE = EntityDataManager.createKey(LocomotiveSteam.class, DataSerializers.FLOAT);
	// Celsius
	private static DataParameter<Float> BOILER_TEMPERATURE = EntityDataManager.createKey(LocomotiveSteam.class, DataSerializers.FLOAT);
	// Map<Slot, TicksToBurn>
	private static DataParameter<NBTTagCompound> BURN_TIME = EntityDataManager.createKey(LocomotiveSteam.class, DataSerializers.COMPOUND_TAG);
	private static DataParameter<NBTTagCompound> BURN_MAX = EntityDataManager.createKey(LocomotiveSteam.class, DataSerializers.COMPOUND_TAG);
	
	public LocomotiveSteam(World world) {
		this(world, null);
	}

	public LocomotiveSteam(World world, String defID) {
		super(world, defID);
		
		this.getDataManager().register(BOILER_PRESSURE, 0f);
		this.getDataManager().register(BOILER_TEMPERATURE, 0f);
		this.getDataManager().register(BURN_TIME, new NBTTagCompound());
		this.getDataManager().register(BURN_MAX, new NBTTagCompound());
	}

	public LocomotiveSteamDefinition getDefinition() {
		return (LocomotiveSteamDefinition) DefinitionManager.getDefinition(defID);
	}

	@Override
	public GuiTypes guiType() {
		return GuiTypes.STEAM_LOCOMOTIVE;
	}
	
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setFloat("boiler_temperature", getBoilerTemperature());
		nbttagcompound.setFloat("boiler_psi", getBoilerPressure());
		nbttagcompound.setTag("burn_time", dataManager.get(BURN_TIME));
		nbttagcompound.setTag("burn_max", dataManager.get(BURN_MAX));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		setBoilerTemperature(nbttagcompound.getFloat("boiler_temperature"));
		setBoilerPressure(nbttagcompound.getFloat("boiler_psi"));
		dataManager.set(BURN_TIME, (NBTTagCompound) nbttagcompound.getTag("burn_time"));
		dataManager.set(BURN_MAX, (NBTTagCompound) nbttagcompound.getTag("burn_max"));
	}
	
	public float getBoilerTemperature() {
		return this.dataManager.get(BOILER_TEMPERATURE);
	}
	private void setBoilerTemperature(float temp) {
		this.dataManager.set(BOILER_TEMPERATURE, temp);
	}
	
	public float getBoilerPressure() {
		return this.dataManager.get(BOILER_PRESSURE);
	}
	private void setBoilerPressure(float temp) {
		this.dataManager.set(BOILER_PRESSURE, temp);
	}
	
	private NBTTagCompound mapToNBT(Map<Integer, Integer> map) {
		NBTTagCompound data = new NBTTagCompound();
		for (Integer slot : map.keySet()) {
			data.setInteger("" + slot, map.get(slot));
		}
		return data;
	}
	private Map<Integer, Integer> NBTtoMap(NBTTagCompound nbt) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (String key : nbt.getKeySet()) {
			map.put(Integer.parseInt(key), nbt.getInteger(key));
		}
		return map;
	}
	
	public Map<Integer, Integer> getBurnTime() {
		return NBTtoMap((NBTTagCompound)this.dataManager.get(BURN_TIME));
	}
	private void setBurnTime(Map<Integer, Integer> burnTime) {
		this.dataManager.set(BURN_TIME, mapToNBT(burnTime));
	}
	public Map<Integer, Integer> getBurnMax() {
		return NBTtoMap((NBTTagCompound)this.dataManager.get(BURN_MAX));
	}
	private void setBurnMax(Map<Integer, Integer> burnMax) {
		this.dataManager.set(BURN_MAX, mapToNBT(burnMax));
	}
	
	
	@Override
	protected int getAvailableHP() {
		return (int) (this.getDefinition().getHorsePower() * Math.pow(this.getBoilerPressure() / this.getDefinition().getMaxPSI(), 3));
	}
	
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		this.setBoilerTemperature(0);
		this.setBoilerPressure(0);
		
		Map<Integer, Integer> burnTime = getBurnTime();
		for (Integer slot : burnTime.keySet()) {
			burnTime.put(slot, 0);
		}
		setBurnTime(burnTime);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			return;
		}
		
		if (!this.isBuilt()) {
			return;
		}
		
		if (this.getCoupled(CouplerType.BACK) instanceof Tender) {
			Tender tender = (Tender) getCoupled(CouplerType.BACK);

			// Only drain 10mb at a time from the tender
			int desiredDrain = 10;
			if (getTankCapacity().MilliBuckets() - getServerLiquidAmount() >= 10) {
				FluidUtil.tryFluidTransfer(this, tender, desiredDrain, true);
			}
			
			//TODO fuel transfer
		}
		
		// Water to steam
		if (getBoilerTemperature() >= 100) {
			if (getTankCapacity().MilliBuckets() > 0) {
				if (rand.nextInt(100) == 0) {
					int outputHorsepower = (int) Math.abs(getThrottle() * getAvailableHP());
					drain(outputHorsepower * 10 / this.getDefinition().getHorsePower(), true);
				}
			}
		}
		
		Map<Integer, Integer> burnTime = getBurnTime();
		Map<Integer, Integer> burnMax = getBurnMax();
		Boolean changedBurnTime = false;
		Boolean changedBurnMax = false;
		
		float waterLevelMB = this.getLiquidAmount();
		float boilerTemperature = this.getBoilerTemperature();
		float boilerPressure = this.getBoilerPressure();
		
		// TODO actual gas and fluid temp calculations
		
		for (int slot = 0; slot < this.cargoItems.getSlots()-2; slot ++) {
			int time = burnTime.containsKey(slot) ? burnTime.get(slot) : 0;
			if (time <= 0) {
				ItemStack stack = this.cargoItems.getStackInSlot(slot);
				if (stack.getCount() <= 0 || !TileEntityFurnace.isItemFuel(stack)) {
					continue;
				}
				time = TileEntityFurnace.getItemBurnTime(stack);
				burnTime.put(slot, time);
				burnMax.put(slot, time);
				stack.setCount(stack.getCount()-1);
				this.cargoItems.setStackInSlot(slot, stack);
				changedBurnMax = true;
			} else {
				burnTime.put(slot, time - 1);
			}
			changedBurnTime = true;
			if (boilerTemperature < 100 || waterLevelMB < this.getTankCapacity().MilliBuckets() * 0.75) {
				boilerTemperature += 100/waterLevelMB;
			}
			if (boilerTemperature >= 100) {
				boilerPressure += 100/waterLevelMB;
				if (rand.nextInt(10) == 0) {
					waterLevelMB -= 1;
				}
			}
		}
		
		if (!changedBurnTime) {
			if (boilerPressure <= 0 || boilerTemperature > 100) {
				//cooling firebox
				boilerTemperature = (float) Math.max(0, boilerTemperature-0.05);
			} else {
				// cooling gas
				boilerPressure = (float) Math.max(0, boilerPressure - 0.05);
			}
		}
		
		float throttle = Math.abs(getThrottle());
		if (throttle != 0 && boilerPressure > 0) {
			boilerPressure = Math.max(0, boilerPressure - throttle * (this.cargoItems.getSlots()-2) * 100/waterLevelMB);
		}
		
		if (boilerPressure > this.getDefinition().getMaxPSI()) {
			// TODO hissing and steam of pressure relief valve
			boilerPressure = this.getDefinition().getMaxPSI();
		}
		
		if (changedBurnTime) {
			setBurnTime(burnTime);
			this.drain(this.getLiquidAmount() - (int)waterLevelMB, true);
		}
		if (changedBurnMax) {
			setBurnMax(burnMax);
		}
		setBoilerTemperature(boilerTemperature);
		setBoilerPressure(boilerPressure);
		
		if (boilerPressure > this.getDefinition().getMaxPSI() * 1.1 || (boilerPressure > this.getDefinition().getMaxPSI() * 0.5 && boilerTemperature > 150)) {
			// 10% over max pressure OR
			// Half max pressure and high boiler temperature
			//EXPLODE
			world.createExplosion(this, this.posX, this.posY, this.posZ, boilerPressure, true);
			world.removeEntity(this);
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