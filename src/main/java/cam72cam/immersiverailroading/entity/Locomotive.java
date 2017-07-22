package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public abstract class Locomotive extends FreightTank {
	private MovingSoundRollingStock hornSound;
	private MovingSoundRollingStock idleSound;
	protected MovingSoundRollingStock runSound;

	private DataParameter<Float> currentMaxSpeedMC = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<String> currentDestination = EntityDataManager.createKey(Locomotive.class, DataSerializers.STRING);
	private DataParameter<String> currentState = EntityDataManager.createKey(Locomotive.class, DataSerializers.STRING);
	private DataParameter<Integer> currentFuelTrain = EntityDataManager.createKey(Locomotive.class, DataSerializers.VARINT);
	private DataParameter<Integer> currentCartsPulled = EntityDataManager.createKey(Locomotive.class, DataSerializers.VARINT);
	private DataParameter<Float> currentMassPulled = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<Float> currentSpeedReductionMC = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<Float> currentAccelReduction = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<Float> currentBrakeReduction = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<Float> currentFuelRate = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private DataParameter<Float> throttle = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);


	public Locomotive(World world, String defID, Fluid... fluids) {
		super(world, defID, fluids);

		this.getDataManager().register(currentMaxSpeedMC, 0f);
		this.getDataManager().register(currentDestination, "");
		this.getDataManager().register(currentFuelTrain, 0);
		this.getDataManager().register(currentCartsPulled, 0);
		this.getDataManager().register(currentMassPulled, 0.0f);
		this.getDataManager().register(currentSpeedReductionMC, 0f);
		this.getDataManager().register(currentAccelReduction, 0.0f);
		this.getDataManager().register(currentBrakeReduction, 0.0f);
		this.getDataManager().register(currentFuelRate, 0.0f);
		this.getDataManager().register(throttle, 0f);

		// hornSound = EnumSounds.get(this.getClass()).getHorn(this);
		// idleSound = EnumSounds.get(this.getClass()).getIdle(this);
		// runSound = EnumSounds.get(this.getClass()).getRun(this);

		this.entityCollisionReduction = 0.99F;
		this.noClip = true;
	}

	/*
	 * 
	 * Things to be overridden
	 */

	public abstract int[] getLocomotiveInventorySizes();
	public abstract int getFuelDiv(int i);
	public abstract void updatePassenger(Entity passenger);
	public abstract double getMaxFuel();
	public abstract int getDefaultFuelConsumption();
	public abstract int getDefaultPower();
	public abstract double getDefaultAccel();
	public abstract double getDefaultBrake();
	public abstract Speed getMaxSpeed();

	/*
	 * 
	 * All this is used in GUI only
	 */

	public Integer getCurrentNumCartsPulled() {
		return dataManager.get(currentCartsPulled);
	}

	public double getCurrentMassPulled() {
		return dataManager.get(currentMassPulled);
	}

	public Speed getCurrentSpeedReduction() {
		return Speed.fromMinecraft(dataManager.get(currentSpeedReductionMC));
	}

	public Float getCurrentAccelReduction() {
		return dataManager.get(currentAccelReduction);
	}

	public Float getCurrentBrakeReduction() {
		return dataManager.get(currentBrakeReduction);
	}

	public String getState() {
		return dataManager.get(currentState);
	}

	public String getDestinationGUI() {
		return dataManager.get(this.currentDestination);
	}

	public int getFuel() {
		return dataManager.get(this.currentFuelTrain);
	}

	public boolean isFuelled() {
		return getFuel() > 0;
	}

	public double getFuelConsumption() {
		return dataManager.get(currentFuelRate);
	}

	/*
	 * 
	 * EntityRollingStock Overrides
	 */

	@Override
	public GuiTypes guiType() {
		return null;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setInteger("fuelTrain", getFuel());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		dataManager.set(currentFuelTrain, nbttagcompound.getInteger("fuelTrain"));
	}

	@Override
	public void setDead() {
		super.setDead();
		System.out.println("Stopping audio");
		Minecraft.getMinecraft().getSoundHandler().stopSound(idleSound);
		Minecraft.getMinecraft().getSoundHandler().stopSound(runSound);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (!world.isRemote) {

			if (ticksExisted % 20 == 0) {
				PullPhysic();
			}
			if (ticksExisted % 100 == 0) {

				int fuelTrain = getFuel();
				fuelTrain -= this.getFuelConsumption();
				if (fuelTrain < 0) {
					fuelTrain = 0;
					motionX *= 0.8;
					motionZ *= 0.8;
				}
				dataManager.set(currentFuelTrain, fuelTrain);
			}
		}
		// runSound.setVolume(getSpeed().minecraft() > 0 ? 1 : 0);
		// idleSound.setVolume(getSpeed().minecraft() > 0 ? 0 : 1);

		// double speed = throttle.get() * 0.02;
		moveRollingStock(0.2);
	}
	
	private void PullPhysic() {
		double totalMass = 0;
		double power = 0;
		double maxRealSpeed = 0;
		double maxSpeedReduction = 0;
		double accelReduction = 0;
		double brakeReduction = 0;
		double currentFuelConsumptionChange = 0;

		List<EntityRollingStock> train = this.getTrain();
		dataManager.set(currentCartsPulled, train.size() - 1);

		/*
		 * TODO MASS for (EntityRollingStock entity : train) { totalMass +=
		 * entity.mass; if (entity instanceof Locomotive) { Locomotive
		 * locomotive = ((Locomotive) entity); // TODO check if the locomotive
		 * is actually ready to be used power += locomotive.getDefaultPower();
		 * maxRealSpeed = Math.min(maxRealSpeed,
		 * locomotive.getDefaultMaxSpeed().metric()); } }
		 */

		maxSpeedReduction = Math.min(maxRealSpeed, totalMass / (power / 400));

		dataManager.set(currentMaxSpeedMC, (float) (maxRealSpeed - maxSpeedReduction));

		double scaledPower = (totalMass / (power / 200)) / 1000 * 0.8;

		accelReduction = Math.min(getDefaultAccel(), scaledPower * totalMass * 1.13);
		double currentAccel = getDefaultAccel() - accelReduction;

		brakeReduction = Math.min(getDefaultBrake(), scaledPower * totalMass);
		double brake = getDefaultBrake() - brakeReduction;

		currentFuelConsumptionChange = Math.min(getDefaultFuelConsumption(), scaledPower * totalMass * 100);
		dataManager.set(currentFuelRate, (float) (getDefaultFuelConsumption() - currentFuelConsumptionChange));
		dataManager.set(currentMassPulled, (float) totalMass);
		dataManager.set(currentSpeedReductionMC, (float) maxSpeedReduction);
		dataManager.set(currentAccelReduction, (float) accelReduction);
		dataManager.set(currentBrakeReduction, (float) brakeReduction);
	}

	/*
	 * 
	 * Misc Helper functions
	 */

	public void soundHorn() {
		if (!Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(hornSound)) {
			Minecraft.getMinecraft().getSoundHandler().playSound(hornSound);
		}
	}

	protected void setState(String state) {
		dataManager.set(currentState, state);
	}

	public void addFuel(double fuel) {
		dataManager.set(currentFuelTrain, (int) Math.min(getMaxFuel(), dataManager.get(currentFuelTrain) + fuel));
	}
}