package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.serialization.TagField;

import java.util.List;
import java.util.OptionalDouble;

public class LocomotiveDiesel extends Locomotive {

	private float soundThrottle;
	private float internalBurn = 0;
	private int turnOnOffDelay = 0;

	@TagSync
	@TagField("ENGINE_TEMPERATURE")
	private float engineTemperature;

	@TagSync
	@TagField("TURNED_ON")
	private boolean turnedOn = false;

	@TagSync
	@TagField("ENGINE_OVERHEATED")
	private boolean engineOverheated = false;

	private int throttleCooldown;
	private int reverserCooldown;

	public LocomotiveDiesel() {
		engineTemperature = ambientTemperature();
	}

	@Override
	public int getInventoryWidth() {
		return getDefinition().isCabCar() ? 0 : 2;
	}

	public float getEngineTemperature() {
		return engineTemperature;
	}
	
	private void setEngineTemperature(float temp) {
		engineTemperature = temp;
	}
	
	public void setTurnedOn(boolean value) {
		turnedOn = value;
		setControlPositions(ModelComponentType.ENGINE_START_X, turnedOn ? 1 : 0);
	}
	
	public boolean isTurnedOn() {
		return turnedOn;
	}
	
	public void setEngineOverheated(boolean value) {
		engineOverheated = value;
	}
	
	public boolean isEngineOverheated() {
		return engineOverheated && Config.ConfigBalance.canDieselEnginesOverheat;
	}
	
	public boolean isRunning() {
		if (!Config.isFuelRequired(gauge)) {
			return isTurnedOn();
		}
		return isTurnedOn() && !isEngineOverheated() && this.getLiquidAmount() > 0;
	}
	
	@Override
	public LocomotiveDieselDefinition getDefinition() {
		return super.getDefinition(LocomotiveDieselDefinition.class);
	}
	
	@Override
	public boolean openGui(Player player) {
		if (!getDefinition().isCabCar() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
			GuiTypes.DIESEL_LOCOMOTIVE.open(player, this);
			return true;
		}
		return false;
	}

	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
		switch(key) {
			case START_STOP_ENGINE:
				if (turnOnOffDelay == 0) {
					turnOnOffDelay = 10;
					setTurnedOn(!isTurnedOn());
				}
				break;
			case REVERSER_UP:
			case REVERSER_ZERO:
			case REVERSER_DOWN:
				if (this.reverserCooldown > 0) {
					return;
				}
				reverserCooldown = 3;
				super.handleKeyPress(source, key, disableIndependentThrottle);
				break;
			case THROTTLE_UP:
			case THROTTLE_ZERO:
			case THROTTLE_DOWN:
				if (this.throttleCooldown > 0) {
					return;
				}
				throttleCooldown = 2;
				super.handleKeyPress(source, key, disableIndependentThrottle);
				break;
			default:
				super.handleKeyPress(source, key, disableIndependentThrottle);
		}
	}

	@Override
	public boolean hasElectricalPower() {
		return this.isRunning() || super.hasElectricalPower();
	}

	@Override
    public boolean internalLightsEnabled() {
		return this.isRunning() || super.internalLightsEnabled();
    }

    @Override
	protected float getReverserDelta() {
		return 0.51f;
	}

	@Override
	public void setThrottle(float newThrottle) {
		int notches = getDefinition().getThrottleNotches();
		if (newThrottle > getThrottle()) {
			super.setThrottle((float) (Math.ceil(newThrottle * notches) / notches));
		} else {
			super.setThrottle((float) (Math.floor(newThrottle * notches) / notches));
		}
	}

	@Override
	public void setReverser(float newReverser) {
		super.setReverser(Math.round(newReverser));

	}

	private int maxHorsePower = this.getDefinition().getHorsePower(gauge);
	private int tractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge);
	private double ratedTopSpeed = this.getDefinition().getMaxSpeed(gauge).metric();
	
	@Override
    public int getAvailableHP() {
		if (isRunning() && (getEngineTemperature() > 75 || !Config.isFuelRequired(gauge))) {
			return this.getDefinition().getHorsePower(gauge);
		}
		return 0;
	}
	
	private double motorEfficiency(Speed speed) {
		double speedPercent = speed.metric() / ratedTopSpeed;
		if (speed.metric() <= ratedTopSpeed) {
			return (.3d * (Math.log(speedPercent + .05d) / Math.log(10))) +.6d;
		}else {
			return Math.pow(4.0d, 0 - ((2 * speedPercent) - 1.9d));
		}
	}
	
	private double torqueDropoff(double output) {
		double torquePercent = (output / tractiveEffort) * .1d;
		return .9d + torquePercent;
	}
	
	@Override
	public double getAppliedTractiveEffort(Speed speed) {
		//speedMetersPerSecond = speed.metric() / 3.6d;
		//powerWatts = (maxHorsePower * getThrottle()) * 745.7d
		double powerCurve = (maxHorsePower * getThrottle() * 745.7d) / (speed.metric() / 3.6d);
		double rpmEfficiency = powerCurve * motorEfficiency(speed);
		double torqueEfficiency = rpmEfficiency * torqueDropoff(rpmEfficiency);
		return getReverser() * Math.min(tractiveEffort, torqueEfficiency) * ConfigBalance.tractionMultiplier;
	}

	@Override
	public void onTick() {
		super.onTick();

		if (turnOnOffDelay > 0) {
			turnOnOffDelay -= 1;
		}

		if (getWorld().isClient) {
			float absThrottle = Math.abs(this.getThrottle());
			if (this.soundThrottle > absThrottle) {
				this.soundThrottle -= Math.min(0.01f, this.soundThrottle - absThrottle);
			} else if (this.soundThrottle < absThrottle) {
				this.soundThrottle += Math.min(0.01f, absThrottle - this.soundThrottle);
			}
			return;
		}

		OptionalDouble control = this.getDefinition().getModel().getControls().stream()
				.filter(x -> x.part.type == ModelComponentType.HORN_CONTROL_X)
				.mapToDouble(this::getControlPosition)
				.max();
		if (control.isPresent() && control.getAsDouble() > 0) {
			this.setHorn(10, hornPlayer);
		}

		float engineTemperature = getEngineTemperature();
		float heatUpSpeed = 0.0029167f * Config.ConfigBalance.dieselLocoHeatTimeScale / 1.7f;
		float ambientDelta = engineTemperature - ambientTemperature();
		float coolDownSpeed = heatUpSpeed * Math.copySign((float)Math.pow(ambientDelta / 130, 2), ambientDelta);

		if (throttleCooldown > 0) {
			throttleCooldown--;
		}

		if (reverserCooldown > 0) {
			reverserCooldown--;
		}

		engineTemperature -= coolDownSpeed;
		
		if (this.getLiquidAmount() > 0 && isRunning()) {
			float consumption = Math.abs(getThrottle()) + 0.05f;
			float burnTime = BurnUtil.getBurnTime(this.getLiquid());
			if (burnTime == 0) {
				burnTime = 200; //Default to 200 for unregistered liquids
			}
			burnTime *= getDefinition().getFuelEfficiency()/100f;
			burnTime *= (Config.ConfigBalance.locoDieselFuelEfficiency / 100f);
			
			while (internalBurn < 0 && this.getLiquidAmount() > 0) {
				internalBurn += burnTime;
				theTank.drain(new FluidStack(theTank.getContents().getFluid(), 1), false);
			}
			
			consumption *= 100;
			consumption *= gauge.scale();
			
			internalBurn -= consumption;
			
			engineTemperature += heatUpSpeed * (Math.abs(getThrottle()) + 0.2f);
			
			if (engineTemperature > 150) {
				engineTemperature = 150;
				setEngineOverheated(true);
			}
		}
		
		if (engineTemperature < 100 && isEngineOverheated()) {
			setEngineOverheated(false);
		}

		setEngineTemperature(engineTemperature);
	}
	
	@Override
	public List<Fluid> getFluidFilter() {
		return BurnUtil.burnableFluids();
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getFuelCapacity(gauge);
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		setEngineTemperature(ambientTemperature());
		setEngineOverheated(false);
		setTurnedOn(false);
	}

	public float getSoundThrottle() {
		return soundThrottle;
	}

	@Override
	public void onDragRelease(Control<?> component) {
		super.onDragRelease(component);
		if (component.part.type == ModelComponentType.ENGINE_START_X) {
			turnedOn = getDefinition().getModel().getControls().stream()
					.filter(c -> c.part.type == ModelComponentType.ENGINE_START_X)
					.allMatch(c -> getControlPosition(c) == 1);
		}
		if (component.part.type == ModelComponentType.REVERSER_X) {
			// Make sure reverser is sync'd
			setControlPositions(ModelComponentType.REVERSER_X, getReverser()/-2 + 0.5f);
		}
	}
}