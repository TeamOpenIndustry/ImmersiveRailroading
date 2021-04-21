package cam72cam.immersiverailroading.entity;

import java.util.UUID;

import cam72cam.immersiverailroading.items.ItemRadioCtrlCard;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.render.SmokeParticle.SmokeParticleData;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.sound.ISound;

public abstract class Locomotive extends FreightTank {
	public ISound bell;

	private static final float throttleNotch = 0.04f;
	private static final float airBrakeNotch = 0.04f;

	@TagField("deadMansSwitch")
	private boolean deadMansSwitch;
	private int deadManChangeTimeout;

	@TagSync
	@TagField("THROTTLE")
	private float throttle = 0;

	@TagSync
	@TagField("AIR_BRAKE")
	private float airBrake = 0;

	@TagSync
	@TagField("HORN")
	protected int hornTime = 0;

	@TagSync
	@TagField(value = "HORN_PLAYER", mapper = StrictTagMapper.class)
	protected UUID hornPlayer = null;

	@TagSync
	@TagField("BELL")
	private int bellTime = 0;

	private int bellKeyTimeout;

	/*
	 * 
	 * Stock Definitions
	 * 
	 */
	
	@Override
	public LocomotiveDefinition getDefinition() {
		return super.getDefinition(LocomotiveDefinition.class);
	}

	/*
	 * 
	 * EntityRollingStock Overrides
	 */

	@Override
	public GuiRegistry.EntityGUI guiType() {
		return null;
	}

	@Override
	public void handleKeyPress(Player source, KeyTypes key) {
		switch(key) {
		case HORN:
			setHorn(10, source.getUUID());
			break;
        case BELL:
            if (this.getDefinition().toggleBell) {
            	if (bellKeyTimeout == 0) {
					bellTime = bellTime != 0 ? 0 : 10;
					bellKeyTimeout = 10;
				}
            } else {
                setBell(10);
            }
            break;
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
		case DEAD_MANS_SWITCH:
			if (deadManChangeTimeout == 0) { 
				deadMansSwitch = !deadMansSwitch;
				if (deadMansSwitch) {
					source.sendMessage(ChatText.DEADMANS_SWITCH_ENABLED.getMessage());
				} else {
					source.sendMessage(ChatText.DEADMANS_SWITCH_DISABLED.getMessage());
				}
				this.deadManChangeTimeout = 5;
			}
			break;
			default:
				super.handleKeyPress(source, key);
		}
	}

    public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_RADIO_CONTROL_CARD)) {
			if(this.gauge.isModel() || this.getDefinition().getRadioCapability() || !Config.ConfigBalance.RadioEquipmentRequired) {
				ItemRadioCtrlCard.Data data = new ItemRadioCtrlCard.Data(player.getHeldItem(hand));
				if (player.isCrouching()) {
					player.sendMessage(data.linked == null ? ChatText.RADIO_NOLINK.getMessage() : ChatText.RADIO_UNLINK.getMessage());
					data.linked = null;
				} else {
					player.sendMessage(data.linked == null ? ChatText.RADIO_LINK.getMessage() : ChatText.RADIO_RELINK.getMessage());
					data.linked = this.getUUID();
				}
				data.write();
			}
			else {
				player.sendMessage(ChatText.RADIO_CANT_LINK.getMessage(this.getDefinition().name()));;
			}
			return ClickResult.ACCEPTED;
		}
		return super.onClick(player, hand);
	}
	
	@Override
	public void onTick() {
		super.onTick();
		
		if (getWorld().isServer) {
			if (deadManChangeTimeout > 0) {
				deadManChangeTimeout -= 1;
			}
			if (bellKeyTimeout > 0) {
				bellKeyTimeout--;
			}
			
			if (deadMansSwitch && !this.getCurrentSpeed().isZero()) {
				boolean hasDriver = false;
				for (Entity entity : this.getPassengers()) {
					if (entity.isPlayer()) {
						hasDriver = true;
						break;
					}
				}
				if (!hasDriver) {
					this.setThrottle(0);
					this.setAirBrake(1);
				}
			}
			if (hornTime > 0) {
				hornTime--;
			} else if (hornPlayer != null) {
				hornPlayer = null;
			}
			if (bellTime > 0 && !this.getDefinition().toggleBell) {
				bellTime--;
			}
		} else {
			if (ConfigSound.soundEnabled && bell != null) {
				if (bellTime != 0 && !bell.isPlaying()) {
					bell.setVolume(0.8f);
					bell.play(getPosition());
				} else if (bellTime == 0 && bell.isPlaying()) {
					bell.stop();
				}

				if (bell.isPlaying()) {
					bell.setPosition(getPosition());
					bell.setVelocity(getVelocity());
					bell.update();
				}
			}
		}

		this.distanceTraveled += simulateWheelSlip();
	}
	
	protected abstract int getAvailableHP();
	
	private double getAppliedTractiveEffort(Speed speed) {
		double locoEfficiency = 0.7f; //TODO config
		double outputHorsepower = Math.abs(Math.pow(getThrottle(), 3) * getAvailableHP());
		
		double tractiveEffortNewtons = (2650.0 * ((locoEfficiency * outputHorsepower) / Math.max(1.4, Math.abs(speed.metric()))));
		return tractiveEffortNewtons;
	}
	
	protected double simulateWheelSlip() {
		double tractiveEffortNewtons = getAppliedTractiveEffort(getCurrentSpeed());
		double staticTractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge) * slipCoefficient() * Config.ConfigBalance.tractionMultiplier;
		staticTractiveEffort *= 1.5; // Fudge factor
		double adhesionFactor = tractiveEffortNewtons / staticTractiveEffort;
		if (adhesionFactor > 1) {
			return Math.copySign(Math.min((adhesionFactor-1)/10, 1), getThrottle());
		}
		return 0;
	}
	
	public double getTractiveEffortNewtons(Speed speed) {	
		if (!this.isBuilt()) {
			return 0;
		}
		
		double tractiveEffortNewtons = getAppliedTractiveEffort(speed);
		double staticTractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge) * slipCoefficient() * Config.ConfigBalance.tractionMultiplier;
		staticTractiveEffort *= 1.5; // Fudge factor
		
		double adhesionFactor = tractiveEffortNewtons / staticTractiveEffort;
		
		if (adhesionFactor > 1) {
			// CRC Handbook of Physical Quantities. Boca Raton, FL: CRC Press, 1997: 145-156.
			double us = 0.74;
			double uk = 0.57;
			tractiveEffortNewtons = staticTractiveEffort * (uk/us) / adhesionFactor;
		}
		
		if (Math.abs(speed.minecraft()) > this.getDefinition().getMaxSpeed(gauge).minecraft()) {
			tractiveEffortNewtons = 0;
		}
		
		return Math.copySign(tractiveEffortNewtons, getThrottle());
	}

	/*
	 * 
	 * Misc Helper functions
	 */
	
	public float getThrottle() {
		return throttle;
	}
	public void setThrottle(float newThrottle) {
		if (this.getThrottle() != newThrottle) {
			throttle = newThrottle;
			triggerResimulate();
		}
	}
	
	public void setHorn(int val, UUID uuid) {
		if (hornPlayer == null && uuid != null) {
			hornPlayer = uuid;
		}
		if (hornPlayer == null || hornPlayer.equals(uuid)) {
			hornTime = val;
		}
	}

	public int getHornTime() {
		return hornTime;
	}

	public Entity getHornPlayer() {
		for (Entity pass : getPassengers()) {
			if (pass.getUUID().equals(hornPlayer)) {
				return pass;
			}
		}
		return null;
	}

	public float getAirBrake() {
		return airBrake;
	}
	public void setAirBrake(float newAirBrake) {
		if (this.getAirBrake() != newAirBrake) {
			airBrake = newAirBrake;
			triggerResimulate();
		}
	}
	public void setBell(int newBell) {
		this.bellTime = newBell;
	}

	public double slipCoefficient() {
		double slipMult = 1.0;
		World world = getWorld();
		if (world.isPrecipitating() && world.canSeeSky(getBlockPosition())) {
			if (world.isRaining(getBlockPosition())) {
				slipMult = 0.6;
			}
			if (world.isSnowing(getBlockPosition())) {
				slipMult = 0.4;
			}
		}
		// Wheel balance messing with friction
		if (this.getCurrentSpeed().metric() != 0) {
			double balance = 1 - 0.004 * Math.abs(this.getCurrentSpeed().metric());
			slipMult *= balance;
		}
		return slipMult;
	}
	
	public float ambientTemperature() {
	    // null during registration
		return internal != null ? getWorld().getTemperature(getBlockPosition()) : 0f;
	}

	protected void addSmoke(Vec3d particlePos, Vec3d motion, int lifespan, float darken, float thickness, double diameter) {
		assert getWorld().isClient;
		Particles.SMOKE.accept(new SmokeParticleData(getWorld(), particlePos, motion, lifespan, darken, thickness, diameter));
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		if (this.bell != null) {
			bell.stop();
		}
	}
}
