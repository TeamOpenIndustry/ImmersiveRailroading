package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.sound.ISound;

import java.util.List;

public class LocomotiveDiesel extends Locomotive {

	private ISound horn;
	private ISound idle;
	private float soundThrottle;
	private float internalBurn = 0;
	private int turnOnOffDelay = 0;
	private float hornVolume = 0;
	private static float hornStep = 0.25f;

	@TagSync
	@TagField("ENGINE_TEMPERATURE")
	private float engineTemperature;

	@TagSync
	@TagField("TURNED_ON")
	private boolean turnedOn = false;

	@TagSync
	@TagField("ENGINE_OVERHEATED")
	private boolean engineOverheated = false;

	public LocomotiveDiesel() {
		engineTemperature = ambientTemperature();
	}

	@Override
	public int getInventoryWidth() {
		return 2;
	}

	public float getEngineTemperature() {
		return engineTemperature;
	}
	
	private void setEngineTemperature(float temp) {
		engineTemperature = temp;
	}
	
	public void setTurnedOn(boolean value) {
		turnedOn = value;
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
	public GuiRegistry.EntityGUI guiType() {
		return GuiTypes.DIESEL_LOCOMOTIVE;
	}

	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Player source, KeyTypes key) {
		switch(key) {
			case START_STOP_ENGINE:
				if (turnOnOffDelay == 0) {
					turnOnOffDelay = 10;
					setTurnedOn(!isTurnedOn());
				}
				break;
			default:
				super.handleKeyPress(source, key);
		}
	}
	
	private void setThrottleMap(EntityRollingStock stock, boolean direction) {
		if (stock instanceof LocomotiveDiesel && ((LocomotiveDiesel)stock).getDefinition().muliUnitCapable) {
			((LocomotiveDiesel) stock).realSetThrottle(this.getThrottle() * (direction ? 1 : -1));
			((LocomotiveDiesel) stock).realAirBrake(this.getAirBrake());
		}
	}
	
	private void realSetThrottle(float newThrottle) {
		if (Config.isFuelRequired(gauge)) {
			newThrottle = Math.copySign(Math.min(Math.abs(newThrottle), this.getEngineTemperature()/100), newThrottle);
		}
		super.setThrottle(newThrottle);
	}
	private void realAirBrake(float newAirBrake) {
		super.setAirBrake(newAirBrake);;
	}
	
	@Override
	public void setThrottle(float newThrottle) {
		realSetThrottle(newThrottle);
		if (this.getDefinition().muliUnitCapable) {
			this.mapTrain(this, true, false, this::setThrottleMap);
		}
	}
	
	@Override
	public void setAirBrake(float newAirBrake) {
		realAirBrake(newAirBrake);
		this.mapTrain(this, true, false, this::setThrottleMap);
	}
	
	@Override
	protected int getAvailableHP() {
		if (isRunning() && (getEngineTemperature() > 75 || !Config.isFuelRequired(gauge))) {
			return this.getDefinition().getHorsePower(gauge);
		}
		return 0;
	}



	@Override
	public void onTick() {
		super.onTick();
		
		if (getWorld().isClient) {
			if (ConfigSound.soundEnabled) {
				if (this.horn == null) {
					this.horn = ImmersiveRailroading.newSound(this.getDefinition().horn, this.getDefinition().getHornSus(), 100, this.soundGauge());
					this.idle = ImmersiveRailroading.newSound(this.getDefinition().idle, true, 80, this.soundGauge());


				}
				if (isRunning()) {
					if (!idle.isPlaying()) {
						this.idle.play(getPosition());
					}
				} else {
					if (idle.isPlaying()) {
						idle.stop();
					}
				}

				if (hornTime != 0 && !horn.isPlaying() && isRunning()) {
					if (this.getDefinition().getHornSus()) {
						hornVolume = 0.5f;
						horn.setVolume(hornVolume);
					}
					horn.play(getPosition());
				}
				else if(hornTime == 0 && horn.isPlaying() && this.getDefinition().getHornSus()){
                    if (hornVolume > 0.5) {
						hornVolume -= 0.25;
						horn.setVolume(hornVolume);
					} else {
						horn.stop();
					}
				}

				if (this.getDefinition().getHornSus() && hornTime != 0 && hornVolume < 1) {
					hornVolume += 0.25;
					horn.setVolume(hornVolume);
				}
				
				float absThrottle = Math.abs(this.getThrottle());
				if (this.soundThrottle > absThrottle) {
					this.soundThrottle -= Math.min(0.01f, this.soundThrottle - absThrottle); 
				} else if (this.soundThrottle < absThrottle) {
					this.soundThrottle += Math.min(0.01f, absThrottle - this.soundThrottle);
				}
	
				if (horn.isPlaying()) {
					horn.setPosition(getPosition());
					horn.setVelocity(getVelocity());
					horn.update();
				}

				
				if (idle.isPlaying()) {
					idle.setPitch(0.7f+this.soundThrottle/4);
					idle.setVolume(Math.max(0.1f, this.soundThrottle));
					idle.setPosition(getPosition());
					idle.setVelocity(getVelocity());
					idle.update();
				}
			}
			
			
			if (!ConfigGraphics.particlesEnabled) {
				return;
			}
			
			Vec3d fakeMotion = this.getVelocity();

			List<ModelComponent> exhausts = this.getDefinition().getModel().getExhaust();
			float throttle = Math.abs(this.getThrottle()) + 0.05f;
			if (exhausts != null && isRunning()) {
				for (ModelComponent exhaust : exhausts) {
					Vec3d particlePos = this.getPosition().add(VecUtil.rotateWrongYaw(exhaust.center.scale(gauge.scale()), this.getRotationYaw() + 180));
					particlePos = particlePos.subtract(fakeMotion);

					double smokeMod = (1 + Math.min(1, Math.max(0.2, Math.abs(this.getCurrentSpeed().minecraft())*2)))/2;
					addSmoke(particlePos, new Vec3d(fakeMotion.x, fakeMotion.y + 0.4 * gauge.scale(), fakeMotion.z), (int) (40 * (1+throttle) * smokeMod), throttle, throttle, exhaust.width() * gauge.scale());
				}
			}
			return;
		}
		
		float engineTemperature = getEngineTemperature();
		float heatUpSpeed = 0.0029167f * Config.ConfigBalance.dieselLocoHeatTimeScale / 1.7f;
		float ambientDelta = engineTemperature - ambientTemperature();
		float coolDownSpeed = heatUpSpeed * Math.copySign((float)Math.pow(ambientDelta / 130, 2), ambientDelta);
		
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
		
		if (turnOnOffDelay > 0) {
			turnOnOffDelay -= 1;
		}
		
		setEngineTemperature(engineTemperature);
	}
	
	@Override
	public void onRemoved() {
		super.onRemoved();

		if (idle != null) {
			idle.stop();
		}
		if (horn != null) {
			horn.stop();
		}
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
}