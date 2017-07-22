package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

	private Float rotationFrontYaw;
	private Float rotationRearYaw;

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
		
		double speed = 0.2;

		if (rotationFrontYaw == null) {
			rotationFrontYaw = rotationYaw;
		}
		if (rotationRearYaw == null) {
			rotationRearYaw = rotationYaw;
		}
		

		Vec3d front = frontBogeyPosition();
		Vec3d rear = rearBogeyPosition();
		Vec3d nextFront = nextPosition(front, this.rotationFrontYaw, nextMovement(this.rotationFrontYaw, speed));
		Vec3d nextRear = nextPosition(rear, this.rotationRearYaw, nextMovement(this.rotationRearYaw, speed));
		Vec3d frontDelta = front.subtractReverse(nextFront);
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		rotationFrontYaw = (float) Math.toDegrees(Math.atan2(-frontDelta.x, frontDelta.z));
		rotationRearYaw = (float) Math.toDegrees(Math.atan2(-rearDelta.x, rearDelta.z));
		rotationFrontYaw = (rotationFrontYaw + 360f) % 360f;
		rotationRearYaw = (rotationRearYaw + 360f) % 360f;

		Vec3d currCenter = between(front, rear);
		Vec3d nextCenter = between(nextFront, nextRear);
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = nextRear.subtractReverse(nextFront);
		
		this.prevRotationYaw = rotationYaw;
		
		this.rotationYaw = (float) Math.toDegrees(Math.atan2(-bogeySkew.x, bogeySkew.z));

		this.rotationYaw = (this.rotationYaw + 360f) % 360f;
		
		
		/*
		Vec3d currCenter = this.getPositionVector();
		Vec3d nextCenter = nextPosition(currCenter, this.rotationYaw, nextMovement(this.rotationYaw, speed));
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = deltaCenter;//nextRear.subtract(nextFront);
		
		this.prevRotationYaw = rotationYaw;
		this.rotationYaw = (float) Math.toDegrees(Math.atan2(-bogeySkew.x, bogeySkew.z));

		this.rotationYaw = (this.rotationYaw + 360f) % 360f;
		*/

		

		this.motionX = deltaCenter.x;
		this.motionZ = deltaCenter.z;

		// Can this run client side in 1.12?
		if (!this.world.isRemote && world.isAirBlock(new BlockPos((int) posX, (int) (this.posY - 0.6), (int) posZ))) {
			// Fall
			if (this.motionY < 1) {
				this.motionY += -0.1;
			}
		} else {
			this.motionY = 0;
		}

		this.prevPosX = posX;
		this.prevPosY = posY;
		this.prevPosZ = posZ;
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
	}	

	private Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}
	
	protected abstract float frontBogeyOffset();
	protected abstract float rearBogeyOffset();

	private Vec3d frontBogeyPosition() {
		//Vec3d front = new Vec3d(0, 0, frontBogeyOffset());
		//front = front.rotateYaw((float) Math.toRadians(this.rotationYaw));
		Vec3d front = new Vec3d(-Math.sin(Math.toRadians(this.rotationYaw)) * frontBogeyOffset(), 0, Math.cos(Math.toRadians(this.rotationYaw)) * frontBogeyOffset());
		return front.addVector(posX, posY, posZ);
	}

	private Vec3d rearBogeyPosition() {
		//Vec3d rear = new Vec3d(0, 0, rearBogeyOffset());
		//rear = rear.rotateYaw((float) Math.toRadians(this.rotationYaw));
		Vec3d rear = new Vec3d(-Math.sin(Math.toRadians(this.rotationYaw)) * rearBogeyOffset(), 0, Math.cos(Math.toRadians(this.rotationYaw)) * rearBogeyOffset());
		return rear.addVector(posX, posY, posZ);
	}

	private TileRail railFromPosition(Vec3d position) {
		TileEntity te = world.getTileEntity(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
		if (te instanceof TileRailGag) {
			return (TileRail) world.getTileEntity(((TileRailGag) te).getParent());
		} else if (te instanceof TileRail) {
			return (TileRail) te;
		} else {
			return null;
		}
	}

	private Vec3d nextMovement(float yaw, double d) {
		double x = -Math.sin(Math.toRadians(yaw));
		double z = Math.cos(Math.toRadians(yaw));
		return new Vec3d(x * d, 0, z * d);
	}

	private Vec3d nextPosition(Vec3d position, float yaw, Vec3d delta) {
		TileRail rail = railFromPosition(position);

		if (rail == null) {
			System.out.println("WARNING OFF TRACK!!!");
			System.out.println(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
			System.out.println(world.getBlockState(new BlockPos(position)).getBlock().getLocalizedName());
			//if (this.ticksExisted > 100) { // HACK
				this.setDead();
			//}
			return position;
		}

		double distance = delta.lengthVector();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = new Vec3d(rail.getCenter()).subtractReverse(position);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = Math.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() + 1; // TODO bake this into
													// BuilderTurn
			// Calculate the angle delta in rad (radians are awesome)
			double yawDelt = distance / radius;

			// Calculate the original next position (won't be accurate, but we
			// use it as an estimate)
			Vec3d nextPos = position.add(delta);
			
			/*for (int i = 0; i < 90; i++) {
				Vec3d check = new Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw + Math.toRadians(i)) * radius, 0, -Math.cos(posRelYaw + Math.toRadians(i)) * radius);
				world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, check.x, check.y, check.z, 0, 0, 0);
			}*/

			// Calculate the two possible next positions (forward on the curve
			// or backward on the curve)
			Vec3d newpos = new Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw + yawDelt) * radius, 0, -Math.cos(posRelYaw + yawDelt) * radius);
			Vec3d newneg = new Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw - yawDelt) * radius, 0, -Math.cos(posRelYaw - yawDelt) * radius);

			// Return whichever position is closest to the estimated next
			// position
			if (newpos.subtract(nextPos).lengthVector() < newneg.subtract(nextPos).lengthVector()) {
				return newpos;
			} else {
				return newneg;
			}
		} else {
			// Good enough for now
			return position.add(delta);
			// Look on either side of the rail for a sibling rail
			/*
			Vec3d side1Pos = position.add(delta.rotateYaw(90));
			Vec3d side2Pos = position.add(delta.rotateYaw(-90));
			TileRail side1Rail = railFromPosition(side1Pos);
			TileRail side2Rail = railFromPosition(side2Pos);
			Vec3d betweenLoc;
			if (side1Rail != null && side1Rail.getParent() == rail.getParent()) {
				betweenLoc = between(new Vec3d(side1Rail.getPos()), new Vec3d(rail.getPos()));
			} else if (side2Rail != null && side2Rail.getParent() == rail.getParent()) {
				betweenLoc = between(new Vec3d(side2Rail.getPos()), new Vec3d(rail.getPos()));
			} else {
				ImmersiveRailroading.logger.error("INVALID RAIL");
				return position.add(delta);
			}
			if (Math.abs(delta.x) > Math.abs(delta.z)) {
				return new Vec3d(position.x + Math.copySign(distance, delta.x), position.y, Math.floor(position.z) + betweenLoc.z);
			} else {
				return new Vec3d(betweenLoc.x, position.y, position.z + Math.copySign(distance, delta.z));
			}
			*/
		}
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