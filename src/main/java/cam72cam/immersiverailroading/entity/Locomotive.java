package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.Speed;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class Locomotive extends FreightTank {
	//private MovingSoundRollingStock hornSound;
	//private MovingSoundRollingStock idleSound;
	//protected MovingSoundRollingStock runSound;

	private static DataParameter<Integer> FUEL = EntityDataManager.createKey(Locomotive.class, DataSerializers.VARINT);
	private static DataParameter<Float> THROTTLE = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private static DataParameter<Float> AIR_BRAKE = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	
	@SideOnly(Side.CLIENT)
	private List<String> debugInfo = new ArrayList<String>();


	public Locomotive(World world, String defID) {
		super(world, defID);

		this.getDataManager().register(FUEL, 0);
		this.getDataManager().register(THROTTLE, 0f);
		this.getDataManager().register(AIR_BRAKE, 0f);

		// hornSound = EnumSounds.get(this.getClass()).getHorn(this);
		// idleSound = EnumSounds.get(this.getClass()).getIdle(this);
		// runSound = EnumSounds.get(this.getClass()).getRun(this);

		this.entityCollisionReduction = 0.99F;
	}

	/*
	 * 
	 * Things to be overridden
	 */

	public abstract int[] getLocomotiveInventorySizes();
	public abstract int getFuelDiv(int i);
	public abstract double getMaxFuel();
	
	/*
	 * 
	 * Stock Definitions
	 * 
	 */
	
	public LocomotiveDefinition getDefinition() {
		return (LocomotiveDefinition) DefinitionManager.getDefinition(defID);
	}

	/*
	 * 
	 * All this is used in GUI only
	 */

	public int getFuel() {
		return dataManager.get(FUEL);
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
		nbttagcompound.setFloat("throttle", dataManager.get(THROTTLE));
		nbttagcompound.setFloat("brake", dataManager.get(AIR_BRAKE));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		dataManager.set(FUEL, nbttagcompound.getInteger("fuelTrain"));
		dataManager.set(THROTTLE, nbttagcompound.getFloat("throttle"));
		dataManager.set(AIR_BRAKE, nbttagcompound.getFloat("brake"));
	}

	@Override
	public void setDead() {
		super.setDead();
		System.out.println("Stopping audio");
		//Minecraft.getMinecraft().getSoundHandler().stopSound(idleSound);
		//Minecraft.getMinecraft().getSoundHandler().stopSound(runSound);
	}
	
	@Override
	public void handleKeyPress(Entity source, KeyTypes key) {
		switch(key) {
		case THROTTLE_UP:
			if (this.dataManager.get(THROTTLE) < 1) {
				this.dataManager.set(THROTTLE, this.dataManager.get(THROTTLE) + 0.1f);
			}
			break;
		case THROTTLE_ZERO:
			this.dataManager.set(THROTTLE, 0f);
			break;
		case THROTTLE_DOWN:
			if (this.dataManager.get(THROTTLE) > -1) {
				this.dataManager.set(THROTTLE, this.dataManager.get(THROTTLE) - 0.1f);
			}
			break;
		case AIR_BRAKE_UP:
			if (this.dataManager.get(AIR_BRAKE) < 1) {
				this.dataManager.set(AIR_BRAKE, this.dataManager.get(AIR_BRAKE) + 0.1f);
			}
			break;
		case AIR_BRAKE_ZERO:
			this.dataManager.set(AIR_BRAKE, 0f);
			break;
		case AIR_BRAKE_DOWN:
			if (this.dataManager.get(AIR_BRAKE) > 0) {
				this.dataManager.set(AIR_BRAKE, this.dataManager.get(AIR_BRAKE) - 0.1f);
			}
			break;
		default:
			super.handleKeyPress(source, key);
			break;
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		/*
		if (!world.isRemote) {
			if (ticksExisted % 100 == 0) {

				int fuelTrain = getFuel();
				fuelTrain -= this.getFuelConsumption();
				if (fuelTrain < 0) {
					fuelTrain = 0;
					motionX *= 0.8;
					motionZ *= 0.8;
				}
			}
		}
		*/
		// runSound.setVolume(getSpeed().minecraft() > 0 ? 1 : 0);
		// idleSound.setVolume(getSpeed().minecraft() > 0 ? 0 : 1);

		moveCoupledRollingStock(getMovement());
	}
	
	protected float getMovement() {
		//http://evilgeniustech.com/idiotsGuideToRailroadPhysics/HorsepowerAndTractiveEffort/
		//http://www.republiclocomotive.com/locomotive-power-calculations.html
		//http://www.wplives.org/forms_and_documents/Air_Brake_Principles.pdf
		
		double outputHorsepower = Math.abs(dataManager.get(THROTTLE) * this.getDefinition().getHorsePower());
		double locoEfficiency = 0.7f; //TODO config
		
		double tractiveEffortNewtons = (2650.0 * ((locoEfficiency * outputHorsepower) / this.getCurrentSpeed().metric()));
		if (Double.isNaN(tractiveEffortNewtons)) {
			tractiveEffortNewtons = 0;
		}
		
		tractiveEffortNewtons = Math.min(tractiveEffortNewtons, this.getDefinition().getStartingTraction() * 4.44822);
		
		//lbs
		double rollingResistanceNewtons = 0;
		double gradeForceNewtons = 0;
		//TODO starting effort
		double massToMove = 0;
		for (EntityCoupleableRollingStock e : this.getTrain()) {
			massToMove += e.getWeight();
			
			rollingResistanceNewtons += 0.0015 * e.getWeight() * 4.44822f;
			
			//Grade forces
			// TODO force while not moving
			double grade = -this.motionY / Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
			if (Double.isNaN(grade)) {
				grade = 0;
			}
			
			// lbs * 1%gradeResistance * grade multiplier
			gradeForceNewtons += (e.getWeight() / 100) * (grade * 100)  * 4.44822f;
		}

		// 0.25 = steel wheel on steel rail
		double brakeAdhesion =  massToMove * 0.25;
		double airBrakeNewtons = brakeAdhesion * dataManager.get(AIR_BRAKE) * 4.44822f;		
		
		// TO KG
		massToMove *=  0.453592f;
		
		double reverseMultiplier = (this.isReverse ? -1 : 1);
		
		// a = f (to newtons) * m (to newtons)
		double tractiveAccell = tractiveEffortNewtons / massToMove;
		double resistanceAccell = rollingResistanceNewtons / massToMove;
		double gradeAccell = gradeForceNewtons / massToMove;
		double brakeAccell = airBrakeNewtons / massToMove;
		
		
		
		double currentMCVelocity = this.getCurrentSpeed().minecraft() * reverseMultiplier;
		double deltaAccellTractiveMCVelocity = Math.copySign(Speed.fromMetric(tractiveAccell).minecraft(), dataManager.get(THROTTLE));
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double deltaAccellRollingResistanceMCVelocity = Math.min(Speed.fromMetric(resistanceAccell).minecraft(), this.getCurrentSpeed().minecraft()) * -reverseMultiplier;
		
		double deltaAccellGradeMCVelocity = Speed.fromMetric(gradeAccell).minecraft();
		
		double deltaAccellBrakeMCVelocity = Math.min(Speed.fromMetric(brakeAccell).minecraft(), this.getCurrentSpeed().minecraft()) * -reverseMultiplier;
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double newMCVelocity = currentMCVelocity + deltaAccellTractiveMCVelocity + deltaAccellRollingResistanceMCVelocity + deltaAccellGradeMCVelocity + deltaAccellBrakeMCVelocity;

		
		if (Math.abs(newMCVelocity) < 0.001) {
			newMCVelocity = 0;
		}
		
		if (Math.abs(newMCVelocity) > this.getDefinition().getMaxSpeed().minecraft()) {
			newMCVelocity = Math.copySign(this.getDefinition().getMaxSpeed().minecraft(), newMCVelocity);
		}

		if(this.ticksExisted % 20 == 0 && world.isRemote) {
			debugInfo = new ArrayList<String>();
			debugInfo.add("Locomotive Output HP: " + outputHorsepower);
			debugInfo.add("Locomotive Tractive Effort N: " + tractiveEffortNewtons);
			debugInfo.add("Train Rolling Resistance N: " + rollingResistanceNewtons);
			debugInfo.add("Train Slope Resistance N: " + gradeForceNewtons);
			debugInfo.add("Train Mass KG: " + massToMove);
			debugInfo.add("Locomotive SPEED M/s: " + currentMCVelocity);
			debugInfo.add("Locomotive Tractive M/s^2: " + deltaAccellTractiveMCVelocity);
			debugInfo.add("Locomotive Rolling M/s^2: " + deltaAccellRollingResistanceMCVelocity);
			debugInfo.add("Locomotive Grade M/s^2: " + deltaAccellGradeMCVelocity);
			debugInfo.add("Locomotive Brake M/s^2: " + deltaAccellBrakeMCVelocity);
			debugInfo.add("Locomotive SPEED M/s: " + newMCVelocity);
		} 
		
		return (float)newMCVelocity;
	}
	
	/*
	 * Client Debug Stuff
	 */
	@SideOnly(Side.CLIENT)
	public List<String> getDebugInfo() {
		return this.debugInfo;
	}

	/*
	 * 
	 * Misc Helper functions
	 */

	public void soundHorn() {
		//if (!Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(hornSound)) {
		//	Minecraft.getMinecraft().getSoundHandler().playSound(hornSound);
		//}
	}

	public void addFuel(double fuel) {
		dataManager.set(FUEL, (int) Math.min(getMaxFuel(), dataManager.get(FUEL) + fuel));
	}
}