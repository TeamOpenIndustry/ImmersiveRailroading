package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import cam72cam.immersiverailroading.entity.physics.Simulation;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.util.VecUtil;

public abstract class EntityCoupleableRollingStock extends EntityMoveableRollingStock {

	static {
		World.onTick(world -> {
			if (world.isClient) {
				return;
			}

			Simulation.simulate(world);
		});
	}

	public enum CouplerType {
		FRONT(0), BACK(180);

		public final float yaw;

		CouplerType(float yaw) {
			this.yaw = yaw;
		}

		public CouplerType opposite() {
			return this == FRONT ? BACK : FRONT;
		}
		
		@Override
		public String toString() {
			return (this == FRONT ? ChatText.COUPLER_FRONT : ChatText.COUPLER_BACK).toString();
		}
	}

	
	@TagSync
	@TagField(value = "CoupledFront", mapper = StrictTagMapper.class)
	private UUID coupledFront = null;
	@TagField("lastKnownFront")
	private Vec3i lastKnownFront = null;
	@TagSync
	@TagField("frontCouplerEngaged")
	private boolean frontCouplerEngaged = true;
	private Vec3d couplerFrontPosition = null;

	@TagSync
	@TagField(value = "CoupledBack", mapper = StrictTagMapper.class)
	private UUID coupledBack = null;
	@TagField("lastKnownRear")
	private Vec3i lastKnownRear= null;
	@TagSync
	@TagField("backCouplerEngaged")
	private boolean backCouplerEngaged = true;
	private Vec3d couplerRearPosition = null;

	@TagSync
	@TagField("hasElectricalPower")
	private boolean hasElectricalPower;
	private boolean hadElectricalPower = false;
	private int gotElectricalPowerTick = -1;

	/*
	 * 
	 * Overrides
	 * 
	 */
	
	@Override
    public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_HOOK) && player.hasPermission(Permissions.COUPLING_HOOK)) {
			if (getWorld().isClient) {
				return ClickResult.ACCEPTED;
			}
			CouplerType coupler = CouplerType.FRONT;
			if (this.getCouplerPosition(CouplerType.FRONT).distanceTo(player.getPosition()) > this.getCouplerPosition(CouplerType.BACK).distanceTo(player.getPosition())) {
				coupler = CouplerType.BACK;
			}
			if (player.isCrouching()) {
				this.setCouplerEngaged(coupler, !this.isCouplerEngaged(coupler));
				if (this.isCouplerEngaged(coupler)) {
					player.sendMessage(ChatText.COUPLER_ENGAGED.getMessage(coupler));
				} else {
					player.sendMessage(ChatText.COUPLER_DISENGAGED.getMessage(coupler));
				}
			} else {
				if (this.isCoupled(coupler) && this.isCouplerEngaged(coupler)) {
					EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
					player.sendMessage(ChatText.COUPLER_STATUS_COUPLED.getMessage(
							coupler,
							coupled.getDefinition().name(),
							coupled.getPosition().x,
							coupled.getPosition().y,
							coupled.getPosition().z
					));
				} else {
					if (this.isCouplerEngaged(coupler)) {
						player.sendMessage(ChatText.COUPLER_STATUS_DECOUPLED_ENGAGED.getMessage(coupler));
					} else {
						player.sendMessage(ChatText.COUPLER_STATUS_DECOUPLED_DISENGAGED.getMessage(coupler));
					}
				}
			}
			return ClickResult.ACCEPTED;
		}
		return super.onClick(player, hand);
	}

	@Override
	public void onTick() {
		super.onTick();

		World world = getWorld();

		if (world.isClient) {
			// Only couple server side
			
			//ParticleUtil.spawnParticle(internal, EnumParticleTypes.REDSTONE, this.getCouplerPosition(CouplerType.FRONT));
			//ParticleUtil.spawnParticle(internal, EnumParticleTypes.SMOKE_NORMAL, this.getCouplerPosition(CouplerType.BACK));

			if (!hadElectricalPower && hasElectricalPower()) {
				gotElectricalPowerTick = getTickCount();
			}

			return;
		}

		for (Control<?> control : getDefinition().getModel().getControls()) {
			if (control.part.type == ModelComponentType.COUPLER_ENGAGED_X) {
				if (control.part.pos.contains(ModelPosition.FRONT)) {
					if (isCouplerEngaged(CouplerType.FRONT) ^ (getControlPosition(control) < 0.5)) {
						setCouplerEngaged(CouplerType.FRONT, getControlPosition(control) < 0.5);
					}
				}
				if (control.part.pos.contains(ModelPosition.REAR)) {
					if (isCouplerEngaged(CouplerType.BACK) ^ (getControlPosition(control) < 0.5)) {
						setCouplerEngaged(CouplerType.BACK, getControlPosition(control) < 0.5);
					}
				}
			}
		}


		if (this.getTickCount() % 5 == 0) {
			hasElectricalPower = false;
			this.mapTrain(this, false, stock -> {
				if (stock instanceof Locomotive && stock.hasElectricalPower()) {
					hasElectricalPower = true;
				}
			});
		}

		hadElectricalPower = hasElectricalPower();

		if (this.getCurrentSpeed().minecraft() != 0 || ConfigDebug.keepStockLoaded) {
			world.keepLoaded(getBlockPosition());
			world.keepLoaded(new Vec3i(this.guessCouplerPosition(CouplerType.FRONT)));
			world.keepLoaded(new Vec3i(this.guessCouplerPosition(CouplerType.BACK)));
			if (this.lastKnownFront != null) {
				world.keepLoaded(this.lastKnownFront);
			}
			if (this.lastKnownRear != null) {
				world.keepLoaded(this.lastKnownRear);
			}
		}

		SimulationState state = getCurrentState();
		if (state != null) {
			setCoupledUUID(CouplerType.FRONT, state.interactingFront);
			setCoupledUUID(CouplerType.BACK, state.interactingRear);
		}
	}

	private Vec3d guessCouplerPosition(CouplerType coupler) {
		return getPosition().add(VecUtil.fromWrongYaw(this.getDefinition().getLength(gauge)/2 * (coupler == CouplerType.FRONT ? 1 : -1), this.getRotationYaw()));
	}


	/*
	 * Coupler Getters and Setters
	 * 
	 */

	public final void setCoupledUUID(CouplerType coupler, UUID id) {
		if (id != null && id.equals(getCoupledUUID(coupler))) {
			// NOP
			return;
		}
		switch (coupler) {
		case FRONT:
			coupledFront = id;
			if (id == null) {
				lastKnownFront = null;
			}
			break;
		case BACK:
			coupledBack = id;
			if (id == null) {
				lastKnownRear = null;
			}
			break;
		}
	}

	public final UUID getCoupledUUID(CouplerType coupler) {
		switch (coupler) {
		case FRONT:
			return coupledFront;
		case BACK:
			return coupledBack;
		default:
			return null;
		}
	}
	
	public EntityCoupleableRollingStock getCoupled(CouplerType coupler) {
		if (this.getCoupledUUID(coupler) != null) {
			return findByUUID(this.getCoupledUUID(coupler));
		}
		return null;
	}

	public CouplerType getCouplerFor(EntityCoupleableRollingStock stock) {
		if (stock == null) {
			return null;
		}
		for (CouplerType coupler : CouplerType.values()) {
			if (stock.getUUID().equals(this.getCoupledUUID(coupler))) {
				return coupler;
			}
		}
		return null;
	}
	
	public boolean isCouplerEngaged(CouplerType coupler) {
		if (coupler == null) {
			return false;
		}
		switch (coupler) {
		case FRONT:
			return frontCouplerEngaged;
		case BACK:
			return backCouplerEngaged;
		default:
			return false;
		}
	}
	
	public void setCouplerEngaged(CouplerType coupler, boolean engaged) {
		switch (coupler) {
		case FRONT:
			frontCouplerEngaged = engaged;
			for (Control<?> control : getDefinition().getModel().getControls()) {
				if (control.part.type == ModelComponentType.COUPLER_ENGAGED_X && control.part.pos.contains(ModelPosition.FRONT)) {
					setControlPosition(control, engaged ? 0 : 1);
				}
			}
			break;
		case BACK:
			backCouplerEngaged = engaged;
			for (Control<?> control : getDefinition().getModel().getControls()) {
				if (control.part.type == ModelComponentType.COUPLER_ENGAGED_X && control.part.pos.contains(ModelPosition.REAR)) {
					setControlPosition(control, engaged ? 0 : 1);
				}
			}
			break;
		}
	}

	/*
	 * Checkers
	 * 
	 */

	public final boolean isCoupled() {
		return isCoupled(CouplerType.FRONT) && isCoupled(CouplerType.BACK);
	}

	public final boolean isCoupled(CouplerType coupler) {
		return getCoupledUUID(coupler) != null;
	}
	
	public final boolean isCoupled(EntityCoupleableRollingStock stock) { 
		return getCouplerFor(stock) != null;
	}

	/*
	 * Decouple
	 * 
	 */

	public void decouple() {
		decouple(CouplerType.FRONT);
		decouple(CouplerType.BACK);
	}

	public void decouple(EntityCoupleableRollingStock stock) {
		if (stock.getUUID().equals(this.getCoupledUUID(CouplerType.FRONT))) {
			decouple(CouplerType.FRONT);
		} else if (stock.getUUID().equals(this.getCoupledUUID(CouplerType.BACK))) {
			decouple(CouplerType.BACK);
		}
	}

	public void decouple(CouplerType coupler) {
		EntityCoupleableRollingStock coupled = getCoupled(coupler);
		
		ImmersiveRailroading.info(this.getUUID() + " decouple " + coupler);

		// Break the coupling
		this.setCoupledUUID(coupler, null);

		// Ask the connected car to do the same
		if (coupled != null) {
			coupled.decouple(this);
		}
	}


	@Override
	protected void clearPositionCache() {
		super.clearPositionCache();
		couplerFrontPosition = null;
		couplerRearPosition = null;
	}

	public Vec3d getCouplerPosition(CouplerType coupler) {
		return getCouplerPosition(coupler, this.getFakeTickPos());
	}

	public Vec3d getCouplerPosition(CouplerType coupler, TickPos pos) {
		
		//Don't ask me why these are reversed...
		if (coupler == CouplerType.FRONT) {
			if (couplerFrontPosition == null) {
				couplerFrontPosition = predictRearBogeyPosition(pos, (float) -(this.getDefinition().getCouplerPosition(coupler, gauge) + this.getDefinition().getBogeyRear(gauge))).add(pos.position).add(0, 1, 0);
			}
			return couplerFrontPosition;
		} else {
			if (couplerRearPosition == null) {
				couplerRearPosition = predictFrontBogeyPosition(pos, (float) (this.getDefinition().getCouplerPosition(coupler, gauge) - this.getDefinition().getBogeyFront(gauge))).add(pos.position).add(0, 1, 0);
			}
			return couplerRearPosition;
		}
	}

	/*
	 * Helpers
	 */
	
	public void triggerTrain() {
		for (EntityCoupleableRollingStock stock : this.getTrain()) {
			stock.triggerResimulate();
		}
	}

	public final List<EntityCoupleableRollingStock> getTrain() {
		return getTrain(true);
	}
	
	

	public final List<EntityCoupleableRollingStock> getTrain(boolean followDisengaged) {
		List<EntityCoupleableRollingStock> train = new ArrayList<EntityCoupleableRollingStock>();
		this.mapTrain(this, followDisengaged, train::add);
		return train;
	}
	
	public final void mapTrain(EntityCoupleableRollingStock prev, boolean followDisengaged, Consumer<EntityCoupleableRollingStock> fn) {
		this.mapTrain(prev, true, followDisengaged, (EntityCoupleableRollingStock e, Boolean b) -> fn.accept(e));
	}
	
	public final void mapTrain(EntityCoupleableRollingStock prev, boolean direction, boolean followDisengaged, BiConsumer<EntityCoupleableRollingStock, Boolean> fn) {
		for (DirectionalStock stock : getDirectionalTrain(followDisengaged)) {
			fn.accept(stock.stock, stock.direction);
		}
	}
	

	public static class DirectionalStock {
		public final EntityCoupleableRollingStock prev;
		public final EntityCoupleableRollingStock stock;
		public final boolean direction;

		public DirectionalStock(EntityCoupleableRollingStock prev, EntityCoupleableRollingStock stock, boolean direction) {
			this.prev = prev;
			this.stock = stock;
			this.direction = direction;
		}
	}
	
	public Collection<DirectionalStock> getDirectionalTrain(boolean followDisengaged) {
		HashSet<UUID> trainMap = new HashSet<UUID>();
		List<DirectionalStock> trainList = new ArrayList<DirectionalStock>();
		
		Function<DirectionalStock, DirectionalStock> next = (DirectionalStock current) -> {
			for (CouplerType coupler : CouplerType.values()) {
				EntityCoupleableRollingStock stock = current.stock;
				boolean direction = current.direction;
				
				if (stock.getCoupledUUID(coupler) == null) {
					continue;
				}
				
				if (trainMap.contains(stock.getCoupledUUID(coupler))) {
					continue;
				}
				
				if (!(followDisengaged || stock.isCouplerEngaged(coupler))) {
					continue;
				}
				
				EntityCoupleableRollingStock coupled = stock.getCoupled(coupler);
				
				if (coupled == null) {
					continue;
				}
				
				CouplerType otherCoupler = coupled.getCouplerFor(stock);
				if (!(followDisengaged || coupled.isCouplerEngaged(otherCoupler))) {
					continue;
				}
				
				return new DirectionalStock(stock, coupled, coupler.opposite() == otherCoupler ? direction : !direction);
			}
			return null;
		};
		
		
		DirectionalStock start = new DirectionalStock(null, this, true);
		trainMap.add(start.stock.getUUID());
		trainList.add(start);
		
		for (int i = 0; i < 2; i ++) {
			// Will fire for both front and back
			
			for (DirectionalStock current = next.apply(start); current != null; current = next.apply(current)) {
				trainMap.add(current.stock.getUUID());
				trainList.add(current);
			}
		}
		
		
		return trainList;
	}

	public EntityCoupleableRollingStock findByUUID(UUID uuid) {
		return getWorld().getEntity(uuid, EntityCoupleableRollingStock.class);
	}
	
	@Override
	public void triggerResimulate() {
	}

	public boolean hasElectricalPower() {
		return this.hasElectricalPower;
	}

    @Override
    public void setControlPosition(Control<?> component, float val) {
        super.setControlPosition(component, val);
        if (component.global) {
			this.mapTrain(this, false, stock -> {
				stock.controlPositions.put(component.controlGroup, this.getControlData(component));
			});
		}
    }

	@Override
	public boolean internalLightsEnabled() {
		return getDefinition().hasInternalLighting() && hasElectricalPower() && (
				gotElectricalPowerTick == -1 ||
						getTickCount() - gotElectricalPowerTick > 15 ||
						((getTickCount() - gotElectricalPowerTick)/(int)((Math.random()+2) * 4)) % 2 == 0
		);
	}
}
