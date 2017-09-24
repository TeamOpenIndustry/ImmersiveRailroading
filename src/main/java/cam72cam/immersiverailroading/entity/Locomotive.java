package cam72cam.immersiverailroading.entity;


import java.util.ArrayList;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.Speed;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class Locomotive extends FreightTank {
	//private MovingSoundRollingStock hornSound;
	//private MovingSoundRollingStock idleSound;
	//protected MovingSoundRollingStock runSound;

	private static DataParameter<Float> THROTTLE = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	private static DataParameter<Float> AIR_BRAKE = EntityDataManager.createKey(Locomotive.class, DataSerializers.FLOAT);
	

	private static final float throttleNotch = 0.04f;
	private static final float airBrakeNotch = 0.04f;
	
	private boolean resimulate = false;


	public Locomotive(World world, String defID) {
		super(world, defID);

		this.getDataManager().register(THROTTLE, 0f);
		this.getDataManager().register(AIR_BRAKE, 0f);

		// hornSound = EnumSounds.get(this.getClass()).getHorn(this);
		// idleSound = EnumSounds.get(this.getClass()).getIdle(this);
		// runSound = EnumSounds.get(this.getClass()).getRun(this);

		this.entityCollisionReduction = 0.99F;
	}
	
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
	 * EntityRollingStock Overrides
	 */

	@Override
	public GuiTypes guiType() {
		return null;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setFloat("throttle", getThrottle());
		nbttagcompound.setFloat("brake", getAirBrake());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		setThrottle(nbttagcompound.getFloat("throttle"));
		setAirBrake(nbttagcompound.getFloat("brake"));
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
			if (getThrottle() < 1) {
				setThrottle(getThrottle() + throttleNotch);
			}
			break;
		case THROTTLE_ZERO:
			setThrottle(0f);
			break;
		case THROTTLE_DOWN:
			if (getThrottle() > -1) {
				setThrottle(getThrottle() - throttleNotch);
			}
			break;
		case AIR_BRAKE_UP:
			if (getAirBrake() < 1) {
				setAirBrake(getAirBrake() + airBrakeNotch);
			}
			break;
		case AIR_BRAKE_ZERO:
			setAirBrake(0f);
			break;
		case AIR_BRAKE_DOWN:
			if (getAirBrake() > 0) {
				setAirBrake(getAirBrake() - airBrakeNotch);
			}
			break;
		default:
			super.handleKeyPress(source, key);
			break;
		}
	}
	
	@Override
	public void triggerResimulate() {
		resimulate = true;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// runSound.setVolume(getSpeed().minecraft() > 0 ? 1 : 0);
		// idleSound.setVolume(getSpeed().minecraft() > 0 ? 0 : 1);
		
		simulateWheelSlip();
		
		if (world.isRemote) {
			return;
		}
		
		if (this.getRemainingPositions() < 20 || resimulate) {
			TickPos lastPos = this.getCurrentTickPos();
			if (lastPos == null) {
				triggerResimulate();
				return;
			}
			
			boolean isStuck = false;
			for (EntityBuildableRollingStock stock : this.getTrain()) {
				if (!stock.areWheelsBuilt()) {
					isStuck = true;
				}
			}
			
			Speed simSpeed = this.getCurrentSpeed();
			if (isStuck) {
				simSpeed = Speed.fromMinecraft(0);
			}
			
			// Clear out the list and re-simulate
			this.positions = new ArrayList<TickPos>();
			positions.add(lastPos);

			for (int i = 0; i < 30; i ++) {
				if (!isStuck) {
					simSpeed = getMovement(simSpeed);
				}
				TickPos pos = this.moveRollingStock(simSpeed.minecraft(), lastPos.tickID + i);
				if (pos.speed.metric() != 0) {
					ChunkManager.flagEntityPos(this.world, new BlockPos(pos.position));
				}
				positions.add(pos);
			}
			
			for (EntityCoupleableRollingStock stock : this.getTrain()) {
				if (stock instanceof Locomotive) {
					Locomotive loco = (Locomotive) stock;
					loco.resimulate = false;
				}
			}
			
			simulateCoupledRollingStock();
		}
	}
	
	protected abstract int getAvailableHP();
	
	private void simulateWheelSlip() {
		if (Math.abs(getTractiveEffortNewtons(this.getCurrentSpeed())) == this.getDefinition().getStartingTractionNewtons()) {
			this.distanceTraveled += Math.copySign(0.05, getThrottle()); //Wheel Slip
		}
	}
	
	private double getTractiveEffortNewtons(Speed speed) {	
		if (!this.isBuilt()) {
			return 0;
		}
		
		double locoEfficiency = 0.7f; //TODO config
		double outputHorsepower = Math.abs(getThrottle() * getAvailableHP());
		
		double tractiveEffortNewtons = (2650.0 * ((locoEfficiency * outputHorsepower) / Math.max(0.0001, Math.abs(speed.metric()))));
		
		if (tractiveEffortNewtons > this.getDefinition().getStartingTractionNewtons()) {
			tractiveEffortNewtons = this.getDefinition().getStartingTractionNewtons();
		}
		
		return Math.copySign(tractiveEffortNewtons, getThrottle());
	}
	
	protected Speed getMovement(Speed speed) {
		//http://evilgeniustech.com/idiotsGuideToRailroadPhysics/HorsepowerAndTractiveEffort/
		//http://www.republiclocomotive.com/locomotive-power-calculations.html
		//http://www.wplives.org/forms_and_documents/Air_Brake_Principles.pdf
		
		// ABS
		speed = Speed.fromMinecraft(Math.abs(speed.minecraft()));
		
		double tractiveEffortNewtons = 0;
		double airBrake = 0;
		
		//lbs
		double rollingResistanceNewtons = 0;
		double gradeForceNewtons = 0;
		//TODO starting effort
		double massToMoveKg = 0;
		for (EntityCoupleableRollingStock e : this.getTrain()) {
			massToMoveKg += e.getWeight();
			
			double stockMassLb = 2.20462 * e.getWeight();
			rollingResistanceNewtons += 0.0015 * stockMassLb * 4.44822f;
			
			//Grade forces
			// TODO force while not moving
			double grade = -e.motionY / Math.sqrt(e.motionX * e.motionX + e.motionZ * e.motionZ);
			if (Double.isNaN(grade)) {
				grade = 0;
			}
			
			// lbs * 1%gradeResistance * grade multiplier
			gradeForceNewtons += (stockMassLb / 100) * (grade * 100)  * 4.44822f;
		}
		
		for (EntityCoupleableRollingStock e : this.getTrain(false)) {
			if (e instanceof Locomotive) {
				Locomotive loco = (Locomotive) e;
				tractiveEffortNewtons += loco.getTractiveEffortNewtons(speed);
				airBrake += loco.getAirBrake();
			}
		}

		// 0.25 = steel wheel on steel rail
		double brakeAdhesion =  massToMoveKg * 0.25;
		double airBrakeNewtons = brakeAdhesion * Math.min(airBrake, 1) * 4.44822f;
		
		double reverseMultiplier = (this.isReverse ? -1 : 1);
		
		// a = f (to newtons) * m (to newtons)
		double tractiveAccell = tractiveEffortNewtons / massToMoveKg;
		double resistanceAccell = rollingResistanceNewtons / massToMoveKg;
		double gradeAccell = gradeForceNewtons / massToMoveKg;
		double brakeAccell = airBrakeNewtons / massToMoveKg;
		
		
		
		double currentMCVelocity = speed.minecraft() * reverseMultiplier;
		double deltaAccellTractiveMCVelocity = Speed.fromMetric(tractiveAccell).minecraft();
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double deltaAccellRollingResistanceMCVelocity = Math.min(Speed.fromMetric(resistanceAccell).minecraft(), speed.minecraft()) * -reverseMultiplier;
		
		double deltaAccellGradeMCVelocity = Speed.fromMetric(gradeAccell).minecraft();
		
		double deltaAccellBrakeMCVelocity = Math.min(Speed.fromMetric(brakeAccell).minecraft(), speed.minecraft()) * -reverseMultiplier;
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double newMCVelocity = currentMCVelocity + deltaAccellTractiveMCVelocity + deltaAccellRollingResistanceMCVelocity + deltaAccellGradeMCVelocity + deltaAccellBrakeMCVelocity;

		if (Math.abs(newMCVelocity) < 0.001) {
			newMCVelocity = 0;
		}
		
		if (Math.abs(newMCVelocity) > this.getDefinition().getMaxSpeed().minecraft()) {
			newMCVelocity = Math.copySign(this.getDefinition().getMaxSpeed().minecraft(), newMCVelocity);
		}

		/*
		if((int)(Math.random()*20) % 20 == 0) {
			System.out.println("Locomotive Tractive Effort N: " + tractiveEffortNewtons);
			System.out.println("Train Rolling Resistance N: " + rollingResistanceNewtons);
			System.out.println("Train Slope Resistance N: " + gradeForceNewtons);
			System.out.println("Train Mass KG: " + massToMoveKg);
			System.out.println("Locomotive SPEED M/s: " + currentMCVelocity);
			System.out.println("Locomotive Tractive M/s^2: " + deltaAccellTractiveMCVelocity);
			System.out.println("Locomotive Rolling M/s^2: " + deltaAccellRollingResistanceMCVelocity);
			System.out.println("Locomotive Grade M/s^2: " + deltaAccellGradeMCVelocity);
			System.out.println("Locomotive Brake M/s^2: " + deltaAccellBrakeMCVelocity);
			System.out.println("Locomotive SPEED M/s: " + newMCVelocity);
		} 
		*/
		
		return Speed.fromMinecraft(newMCVelocity);
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
	
	public float getThrottle() {
		return dataManager.get(THROTTLE);
	}
	public void setThrottle(float newThrottle) {
		dataManager.set(THROTTLE, newThrottle);
		triggerResimulate();
	}
	
	public float getAirBrake() {
		return dataManager.get(AIR_BRAKE);
	}
	public void setAirBrake(float newAirBrake) {
		dataManager.set(AIR_BRAKE, newAirBrake);
		triggerResimulate();
	}
}