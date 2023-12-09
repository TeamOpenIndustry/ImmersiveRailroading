package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.items.ItemRadioCtrlCard;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.physics.MovementTrack;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.OptionalDouble;
import java.util.UUID;

import static cam72cam.immersiverailroading.library.PhysicalMaterials.*;

public abstract class Locomotive extends FreightTank {
	private static final float throttleDelta = 0.04f;
	private static final float trainBrakeNotch = 0.04f;

	@TagField("deadMansSwitch")
	private boolean deadMansSwitch;
	private int deadManChangeTimeout;

	@TagSync
	@TagField("THROTTLE")
	private float throttle = 0;

	@TagSync
	@TagField("REVERSER")
	private float reverser = 0;

	@TagSync
	@TagField("AIR_BRAKE")
	private float trainBrake = 0;

	@TagSync
	@TagField("HORN")
	protected int hornTime = 0;

	@TagSync
	@TagField(value = "HORN_PLAYER", mapper = StrictTagMapper.class)
	protected UUID hornPlayer = null;
	@TagSync
	@TagField(value = "HORN_PULL")
	public float hornPull;

	@TagSync
	@TagField("BELL")
	private int bellTime = 0;
	private boolean bellControl = false;

	private int bellKeyTimeout;

	@TagSync
	@TagField("cogging")
	private boolean cogging = false;

	private boolean slipping = false;

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
	public boolean openGui(Player player) {
		return false;
	}

	@Override
	public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {

		if (disableIndependentThrottle) {
			switch (key) {
				case THROTTLE_UP:
					key = KeyTypes.REVERSER_UP;
					break;
				case THROTTLE_ZERO:
					key = KeyTypes.REVERSER_ZERO;
					break;
				case THROTTLE_DOWN:
					key = KeyTypes.REVERSER_DOWN;
					break;
				case REVERSER_UP:
				case REVERSER_ZERO:
				case REVERSER_DOWN:
					return;
			}
		} else if (getDefinition().isLinkedBrakeThrottle()) {
			switch (key) {
				case THROTTLE_UP:
					if (getTrainBrake() > 0) {
						key = KeyTypes.TRAIN_BRAKE_DOWN;
					}
					break;
				case THROTTLE_ZERO:
					setTrainBrake(0);
					break;
				case THROTTLE_DOWN:
					if (getThrottle() == 0) {
						key = KeyTypes.TRAIN_BRAKE_UP;
					}
					break;
				case TRAIN_BRAKE_UP:
				case TRAIN_BRAKE_ZERO:
				case TRAIN_BRAKE_DOWN:
					return;
			}
		}

		boolean linkThrottleReverser = forceLinkThrottleReverser() || disableIndependentThrottle;

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
			setThrottle(getThrottle() + throttleDelta);
			break;
		case THROTTLE_ZERO:
			setThrottle(0f);
			break;
		case THROTTLE_DOWN:
			setThrottle(getThrottle() - throttleDelta);
			break;
		case REVERSER_UP:
			if (linkThrottleReverser) {
				float mixed = getThrottle() * (getReverser() >= 0 ? 1 : -1);
				if (mixed < 0) {
					setRealThrottle(-mixed - throttleDelta);
					setReverser(-1);
				} else {
					setRealThrottle(mixed + throttleDelta);
					setReverser(1);
				}
			} else {
				setReverser(getReverser() + getReverserDelta());
			}
			break;
		case REVERSER_ZERO:
			if (linkThrottleReverser) {
				setRealThrottle(0);
			}
			setReverser(0f);
			break;
		case REVERSER_DOWN:
			if (linkThrottleReverser) {
				float mixed = getThrottle() * (getReverser() >= 0 ? 1 : -1);
				if (mixed > 0) {
					setRealThrottle(mixed - throttleDelta);
					setReverser(1);
				} else {
					setRealThrottle(-mixed + throttleDelta);
					setReverser(-1);
				}
			} else {
				setReverser(getReverser() - getReverserDelta());
			}
			break;
		case TRAIN_BRAKE_UP:
			setTrainBrake(getTrainBrake() + trainBrakeNotch);
			break;
		case TRAIN_BRAKE_ZERO:
			setTrainBrake(0f);
			break;
		case TRAIN_BRAKE_DOWN:
			setTrainBrake(getTrainBrake() - trainBrakeNotch);
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
				super.handleKeyPress(source, key, disableIndependentThrottle);
		}
	}

	protected boolean forceLinkThrottleReverser() {
		return false;
	}


	protected float getReverserDelta() {
		return throttleDelta;
	}

	public void onDrag(Control<?> component, double newValue) {
		super.onDrag(component, newValue);
		//System.out.println("DRAG " + component + ": "+ getControlPosition(component));
		switch (component.part.type) {
			case THROTTLE_X:
				setThrottle(getControlPosition(component));
				break;
			case TRAIN_BRAKE_X:
				if (getDefinition().isLinearBrakeControl()) {
					setTrainBrake(getControlPosition(component));
				}
				break;
			case REVERSER_X:
				setReverser((0.5f-getControlPosition(component))*2);
				break;
			case THROTTLE_BRAKE_X:
				// value 0     0.5     1
				// throt 0      0      1
				// brake 1      0      0
				setTrainBrake(1 - getControlPosition(component)*2);
				setThrottle(getControlPosition(component)*2 - 1);
				break;
		}
	}

	@Override
	public void onDragRelease(Control<?> control) {
		super.onDragRelease(control);
		if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.TRAIN_BRAKE_X) {
			setControlPosition(control, 0.5f);
		}
	}

	@Override
	protected float defaultControlPosition(Control<?> control) {
		switch (control.part.type) {
			case THROTTLE_BRAKE_X:
			case REVERSER_X:
				return 0.5f;
			case TRAIN_BRAKE_X:
				return getDefinition().isLinearBrakeControl() ? 0 : 0.5f;
			default:
				return super.defaultControlPosition(control);
		}
	}

    @Override
    public boolean playerCanDrag(Player player, Control<?> control) {
        if (!super.playerCanDrag(player, control)) {
        	return false;
		}
        switch (control.part.type) {
			case THROTTLE_X:
			case REVERSER_X:
			case TRAIN_BRAKE_X:
			case INDEPENDENT_BRAKE_X:
			case THROTTLE_BRAKE_X:
			case BELL_CONTROL_X:
			case WHISTLE_CONTROL_X:
			case HORN_CONTROL_X:
			case ENGINE_START_X:
				return player.hasPermission(Permissions.LOCOMOTIVE_CONTROL);
			default:
				return true;
		}
    }

    public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_RADIO_CONTROL_CARD) && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
			if (getWorld().isClient) {
				return ClickResult.ACCEPTED;
			}
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
	public boolean canFitPassenger(Entity passenger) {
		if (passenger instanceof Player && !((Player) passenger).hasPermission(Permissions.BOARD_LOCOMOTIVE)) {
			return false;
		}
		return super.canFitPassenger(passenger);
	}

	@Override
	public void onTick() {
		super.onTick();
		
		if (getWorld().isServer) {
			sync.setInterval(5);
			for (Control<?> control : getDefinition().getModel().getControls()) {
				// Logic duplicated in Readouts#setValue
				if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.TRAIN_BRAKE_X) {
					setTrainBrake(Math.max(0, Math.min(1, getTrainBrake() + (getControlPosition(control) - 0.5f) / 8)));
				}
			}

			if (deadManChangeTimeout > 0) {
				deadManChangeTimeout -= 1;
			}
			if (bellKeyTimeout > 0) {
				bellKeyTimeout--;
			}
			
			if (deadMansSwitch && !this.getCurrentSpeed().isZero()) {
				boolean hasDriver = this.getPassengers().stream().anyMatch(Entity::isPlayer);
				if (!hasDriver) {
					this.setThrottle(0);
					this.setTrainBrake(1);
				}
			}
			if (hornTime > 0) {
				hornTime--;
			} else if (hornPlayer != null) {
				hornPlayer = null;
			}
			if (hornTime == 0) {
				hornPull = 0;
			}
			OptionalDouble control = this.getDefinition().getModel().getControls().stream()
					.filter(x -> x.part.type == ModelComponentType.BELL_CONTROL_X)
					.mapToDouble(this::getControlPosition)
					.max();
			if (control.isPresent() && control.getAsDouble() > 0) {
				bellTime = 10;
				bellControl = true;
			}
			if (bellTime > 0 && (!this.getDefinition().toggleBell || bellControl)) {
				bellTime--;
				if (bellTime == 0) {
					bellControl = false;
				}
			}
		}

		this.distanceTraveled += simulateWheelSlip();

		if (getWorld().isServer) {
			setControlPosition("REVERSERFORWARD", getReverser() > 0 ? 1 : 0);
			setControlPosition("REVERSERNEUTRAL", getReverser() == 0 ? 1 : 0);
			setControlPosition("REVERSERBACKWARD", getReverser() < 0 ? 1 : 0);
		}

		if (getWorld().isServer) {
			if (getDefinition().isCog() && getTickCount() % 20 == 0) {
				SimulationState state = getCurrentState();
				if (state != null) {
					ITrack found = MovementTrack.findTrack(getWorld(), state.couplerPositionFront, state.yaw, gauge.value());
					if (found instanceof TileRailBase) {
						TileRailBase onTrack = (TileRailBase) found;
						cogging = onTrack.isCog();
					}
				}
			}
		}
	}

	/** Force applied between the wheels and the rails */
	public abstract double getAppliedTractiveEffort(Speed speed);

	/** Maximum force that can be between the wheels and the rails before it slips */
	private double getStaticTractiveEffort(Speed speed) {
		return (Config.ConfigBalance.FuelRequired ? this.getWeight() : this.getMaxWeight()) // KG
				* 9.8 // M/S/S
				* (slipping ? STEEL.kineticFriction(STEEL)/2 : STEEL.staticFriction(STEEL))
				* slipCoefficient(speed)
				/ getDefinition().factorOfAdhesion()
				* Config.ConfigBalance.tractionMultiplier;
	}
	
	protected double simulateWheelSlip() {
		if (cogging) {
			return 0;
		}

		double adhesionFactor = Math.abs(getAppliedTractiveEffort(getCurrentSpeed())) /
								getStaticTractiveEffort(getCurrentSpeed());
		slipping = adhesionFactor > 1;
		if (slipping) {
			return Math.copySign((adhesionFactor-1)/5, getReverser());
		}
		return 0;
	}
	
	public double getTractiveEffortNewtons(Speed speed) {	
		if (!this.isBuilt()) {
			return 0;
		}

		if (Math.abs(speed.minecraft()) > this.getDefinition().getMaxSpeed(gauge).minecraft()) {
			return 0;
		}

		double appliedTractiveEffort = getAppliedTractiveEffort(speed);

		if (!cogging && Math.abs(appliedTractiveEffort) > 0) {
			double staticTractiveEffort = getStaticTractiveEffort(speed);

			if (Math.abs(appliedTractiveEffort) > staticTractiveEffort) {
				// This is a guess, but seems to be fairly accurate

				// Reduce tractive effort to max static translated into kinetic
				double tractiveEffortNewtons = staticTractiveEffort /
						STEEL.staticFriction(STEEL) *
						STEEL.kineticFriction(STEEL);

				// How badly tractive effort is overwhelming static effort
				tractiveEffortNewtons *= staticTractiveEffort / tractiveEffortNewtons;

				return Math.copySign(tractiveEffortNewtons, appliedTractiveEffort);
			}
		}

		return appliedTractiveEffort;
	}

	@Override
	public double getBrakeSystemEfficiency() {
		if (cogging) {
			return 10;
		}
		return super.getBrakeSystemEfficiency();
	}

	@Override
	public double getBrakeAdhesionEfficiency() {
		if (cogging) {
			return 10;
		}
		return super.getBrakeAdhesionEfficiency();
	}
	/*
	 * 
	 * Misc Helper functions
	 */

	private void copySettings(EntityRollingStock stock, boolean direction) {
		if (stock instanceof Locomotive && ((Locomotive)stock).getDefinition().muliUnitCapable) {
			((Locomotive) stock).setRealThrottle(this.getThrottle());
			((Locomotive) stock).setRealReverser(this.getReverser() * (direction ? 1 : -1));
			((Locomotive) stock).setRealTrainBrake(this.getTrainBrake());
			((Locomotive) stock).setRealIndependentBrake(this.getIndependentBrake());
		}
	}
	
	public float getThrottle() {
		return throttle;
	}
	public void setThrottle(float newThrottle) {
		setRealThrottle(newThrottle);
		if (this.getDefinition().muliUnitCapable) {
			this.mapTrain(this, true, false, this::copySettings);
		}
	}
	private void setRealThrottle(float newThrottle) {
		newThrottle = Math.min(1, Math.max(0, newThrottle));
		if (this.getThrottle() != newThrottle) {
			setControlPositions(ModelComponentType.THROTTLE_X, newThrottle);
			throttle = newThrottle;
			setControlPositions(ModelComponentType.THROTTLE_BRAKE_X, getThrottle()/2 + (1- getTrainBrake())/2);
		}
	}

	public float getReverser() {
		return reverser;
	}
	public void setReverser(float newReverser) {
		setRealReverser(newReverser);
		if (this.getDefinition().muliUnitCapable) {
			this.mapTrain(this, true, false, this::copySettings);
		}
	}
	private void setRealReverser(float newReverser){
		newReverser = Math.min(1, Math.max(-1, newReverser));

		if (this.getReverser() != newReverser) {
			setControlPositions(ModelComponentType.REVERSER_X, newReverser/-2 + 0.5f);
			reverser = newReverser;
		}
	}

	public void setHorn(int val, UUID uuid) {
		if (uuid == null) {
			// Legacy API
			hornPull = 1;
		}

		if (hornPlayer == null && uuid != null) {
			hornPlayer = uuid;
		}
		if (hornPlayer == null || hornPlayer.equals(uuid)) {
			hornTime = val;
		}
	}

	public void setHorn(int time, float value) {
		hornTime = time;
		hornPull = value;
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

	public float getHornPull() {
		if (getHornPlayer() != null) {
			return (getHornPlayer().getRotationPitch() + 90) / 180;
		}
		double control = this.getDefinition().getModel().getControls().stream()
				.filter(x -> x.part.type == ModelComponentType.WHISTLE_CONTROL_X)
				.mapToDouble(this::getControlPosition)
				.max().orElse(0);

		return Math.max((float)control, hornPull);
	}

	@Deprecated
	public float getAirBrake() {
		return getTrainBrake();
	}
	public float getTrainBrake() {
		return trainBrake;
	}
	@Deprecated
	public void setAirBrake(float value) {
		setTrainBrake(value);
	}
	public void setTrainBrake(float newTrainBrake) {
		setRealTrainBrake(newTrainBrake);
		if (this.getDefinition().muliUnitCapable) {
			this.mapTrain(this, true, false, this::copySettings);
		}
	}
	private void setRealTrainBrake(float newTrainBrake) {
		newTrainBrake = Math.min(1, Math.max(0, newTrainBrake));
		if (this.getTrainBrake() != newTrainBrake) {
			if (getDefinition().isLinearBrakeControl()) {
				setControlPositions(ModelComponentType.TRAIN_BRAKE_X, newTrainBrake);
			}
			trainBrake = newTrainBrake;
			setControlPositions(ModelComponentType.THROTTLE_BRAKE_X, getThrottle()/2 + (1- getTrainBrake())/2);
		}
	}

	@Override
	public void setIndependentBrake(float newIndependentBrake) {
		setRealIndependentBrake(newIndependentBrake);
		if (this.getDefinition().muliUnitCapable) {
			this.mapTrain(this, true, false, this::copySettings);
		}
	}
	private void setRealIndependentBrake(float newIndependentBrake) {
		super.setIndependentBrake(newIndependentBrake);
	}


	public int getBell() {
		return bellTime;
	}
	public void setBell(int newBell) {
		this.bellTime = newBell;
	}

	public double slipCoefficient(Speed speed) {
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
		if (speed.metric() != 0) {
			double balance = 1d/(Math.abs(speed.metric())+300) / (1d/300);
			slipMult *= balance;
		}
		return slipMult;
	}

	public abstract boolean providesElectricalPower();

	@Override
	public boolean hasElectricalPower() {
		return super.hasElectricalPower() || providesElectricalPower();
	}

	public float ambientTemperature() {
	    // null during registration
		return internal != null ? getWorld().getTemperature(getBlockPosition()) : 0f;
	}
}
