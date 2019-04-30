package cam72cam.immersiverailroading.entity;

import java.util.UUID;

import cam72cam.mod.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.TagCompound;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.sound.ISound;

public abstract class Locomotive extends FreightTank {

	protected final static String THROTTLE = "THROTTLE";
	protected final static String AIR_BRAKE = "AIR_BRAKE";
	protected final static String HORN = "HORN";
	protected final static String HORN_PLAYER = "HORN_PLAYER";
	protected final static String BELL = "BELL";

	public ISound bell;

	private static final float throttleNotch = 0.04f;
	private static final float airBrakeNotch = 0.04f;
	
	private boolean deadMansSwitch;
	private int deadManChangeTimeout;

	private int bellKeyTimeout;

	public Locomotive(ModdedEntity entity) {
		super(entity);

		sync.setFloat(THROTTLE, 0f);
		sync.setFloat(AIR_BRAKE, 0f);
		sync.setInteger(HORN, 0);
		sync.setInteger(BELL, 0);
		sync.setUUID(HORN_PLAYER, null);
	}
	
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
	public GuiTypes guiType() {
		return null;
	}

	@Override
	public void save(TagCompound data) {
		super.save(data);
		data.setFloat("throttle", getThrottle());
		data.setFloat("brake", getAirBrake());
		data.setBoolean("deadMansSwitch", deadMansSwitch);
		data.setInteger("bell", getBell());
	}

	public void load(TagCompound data) {
		super.load(data);
		setThrottle(data.getFloat("throttle"));
		setAirBrake(data.getFloat("brake"));
		deadMansSwitch = data.getBoolean("deadMansSwitch");
		setBell(data.getInteger("bell"));
	}
	
	@Override
	public void handleKeyPress(Player source, KeyTypes key, boolean sprinting) {
		switch(key) {
		case HORN:
			setHorn(10, source.getUUID());
			break;
        case BELL:
            if (this.getDefinition().toggleBell) {
            	if (bellKeyTimeout == 0) {
					if (getBell() != 0) {
						setBell(0);
					} else {
						setBell(10);
					}
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
			super.handleKeyPress(source, key, sprinting);
			break;
		}
	}

    public ClickResult onClick(Player player, Hand hand) {
		if (player.getHeldItem(hand).item == IRItems.ITEM_RADIO_CONTROL_CARD) {
			if(this.gauge.isModel() || this.getDefinition().getRadioCapability() || !Config.ConfigBalance.RadioEquipmentRequired) {
				TagCompound cardNBT = player.getHeldItem(hand).getTagCompound();
				if(cardNBT == null) { 
					player.getHeldItem(hand).setTagCompound(new TagCompound());
					cardNBT = player.getHeldItem(hand).getTagCompound();
				}
				if (player.isCrouching()) {
					if (!cardNBT.hasKey("linked_uuid")) {
						player.sendMessage(ChatText.RADIO_NOLINK.getMessage());
					} else {
						cardNBT.remove("linked_uuid");
						player.sendMessage(ChatText.RADIO_UNLINK.getMessage());
					}
				} else {
					if (!cardNBT.hasKey("linked_uuid")) {
						cardNBT.setString("linked_uuid",this.getUUID().toString());
						player.sendMessage(ChatText.RADIO_LINK.getMessage());
					} else {
						cardNBT.setString("linked_uuid",this.getUUID().toString());
						player.sendMessage(ChatText.RADIO_RELINK.getMessage());
					}
				}
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
			if (sync.getInteger(HORN) > 0) {
				sync.setInteger(HORN, sync.getInteger(HORN)-1);
				sync.send();
			} else if (sync.get(HORN_PLAYER) != null) {
				sync.set(HORN_PLAYER, null);
				sync.send();
			}
			if (getBell() > 0 && !this.getDefinition().toggleBell) {
				setBell(getBell()-1);
			}
		} else {
			if (ConfigSound.soundEnabled && bell != null) {
				if (getBell() != 0 && !bell.isPlaying()) {
					bell.play(getPosition());
				} else if (getBell() == 0 && bell.isPlaying()) {
					bell.stop();
				}
			}
		}

		simulateWheelSlip();
	}
	
	protected abstract int getAvailableHP();
	
	private double getAppliedTractiveEffort(Speed speed) {
		double locoEfficiency = 0.7f; //TODO config
		double outputHorsepower = Math.abs(Math.pow(getThrottle(), 3) * getAvailableHP());
		
		double tractiveEffortNewtons = (2650.0 * ((locoEfficiency * outputHorsepower) / Math.max(1.4, Math.abs(speed.metric()))));
		return tractiveEffortNewtons;
	}
	
	private void simulateWheelSlip() {
		double tractiveEffortNewtons = getAppliedTractiveEffort(getCurrentSpeed());
		double staticTractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge) * slipCoefficient() * Config.ConfigBalance.tractionMultiplier;
		staticTractiveEffort *= 1.5; // Fudge factor
		double adhesionFactor = tractiveEffortNewtons / staticTractiveEffort;
		if (adhesionFactor > 1) {
			this.distanceTraveled += Math.copySign(Math.min((adhesionFactor-1)/10, 1), getThrottle());
		}
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
		return sync.getFloat(THROTTLE);
	}
	public void setThrottle(float newThrottle) {
		if (this.getThrottle() != newThrottle) {
			sync.setFloat(THROTTLE, newThrottle);
			sync.send();
			triggerResimulate();
		}
	}
	
	public void setHorn(int val, UUID uuid) {
		UUID currentPlayer = sync.getUUID(HORN_PLAYER);
		if (currentPlayer == null && uuid != null) {
			currentPlayer = uuid;
			sync.setUUID(HORN_PLAYER, uuid);
		}
		if (currentPlayer == null || currentPlayer == uuid) {
			sync.setInteger(HORN, val);
		}
		sync.send();
	}

	public float getAirBrake() {
		return sync.getFloat(AIR_BRAKE);
	}
	public void setAirBrake(float newAirBrake) {
		if (this.getAirBrake() != newAirBrake) {
			sync.setFloat(AIR_BRAKE, newAirBrake);
			sync.send();
			triggerResimulate();
		}
	}
	public int getBell() {
		return sync.getInteger(BELL);
	}
	public void setBell(int newBell) {
		if (this.getBell() != newBell) {
			sync.setInteger(BELL, newBell);
			sync.send();
		}
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
		return getWorld().getTemperature(getBlockPosition());
	}
	
}
