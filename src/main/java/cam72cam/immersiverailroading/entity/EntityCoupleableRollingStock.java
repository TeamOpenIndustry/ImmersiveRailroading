package cam72cam.immersiverailroading.entity;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import cam72cam.immersiverailroading.entity.physics.Consist;
import cam72cam.immersiverailroading.entity.physics.Simulation;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.net.SoundPacket;
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
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.util.VecUtil;

public abstract class EntityCoupleableRollingStock extends EntityMoveableRollingStock {

	static {
		World.onTick(world -> {
			if (world.isClient) {
				return;
			}

			if (ConfigDebug.lagServer > 0) {
				try {
					Thread.sleep(ConfigDebug.lagServer);
				} catch (InterruptedException e) {
					// pass
				}
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
	@TagSync
	@TagField("frontCouplerEngaged")
	private boolean frontCouplerEngaged = true;

	@TagSync
	@TagField(value = "CoupledBack", mapper = StrictTagMapper.class)
	private UUID coupledBack = null;
	@TagSync
	@TagField("backCouplerEngaged")
	private boolean backCouplerEngaged = true;


	@TagField(value = "consist", mapper = Consist.TagMapper.class)
	public Consist consist = new Consist(Collections.emptyList(), Collections.emptyList());

    @TagField("lastKnownFront")
    public Vec3i lastKnownFront = null;
	@TagField("lastKnownRear")
	public Vec3i lastKnownRear = null;

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
				EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
				System.out.println(this.getCoupledUUID(coupler));
				if (this.isCoupled(coupler) && this.isCouplerEngaged(coupler) && coupled != null) {
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
			this.mapTrain(this, false, stock ->
					hasElectricalPower = hasElectricalPower ||
							stock instanceof Locomotive && ((Locomotive) stock).providesElectricalPower()
			);
		}

		hadElectricalPower = hasElectricalPower();

		if (this.getCurrentState() != null && !this.getCurrentState().atRest || ConfigDebug.keepStockLoaded) {
			keepLoaded();
		}

		SimulationState state = getCurrentState();
		if (state != null) {
			setCoupledUUID(CouplerType.FRONT, state.interactingFront);
			setCoupledUUID(CouplerType.BACK, state.interactingRear);
			consist = state.consist;

			if (getCoupledUUID(CouplerType.FRONT) != null) {
				EntityCoupleableRollingStock front = getCoupled(CouplerType.FRONT);
				if (front != null) {
					lastKnownFront = front.getBlockPosition();
				}
			} else {
				lastKnownFront = null;
			}
			if (getCoupledUUID(CouplerType.BACK) != null) {
				EntityCoupleableRollingStock rear = getCoupled(CouplerType.BACK);
				if (rear != null) {
					lastKnownRear = rear.getBlockPosition();
				}
			} else {
				lastKnownRear = null;
			}
		}
	}

	public void keepLoaded() {
		World world = getWorld();
		world.keepLoaded(getBlockPosition());
		if (getCurrentState() != null && !getCurrentState().atRest) {
			world.keepLoaded(new Vec3i(this.guessCouplerPosition(CouplerType.FRONT)));
			world.keepLoaded(new Vec3i(this.guessCouplerPosition(CouplerType.BACK)));
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
		UUID target = coupler == CouplerType.FRONT ? coupledFront : coupledBack;
		if (Objects.equals(target, id)) {
			return;
		}
		if (target == null && isCouplerEngaged(coupler)) {
			// Technically this fires the coupling sound twice (once for each entity)
			new SoundPacket(getDefinition().couple_sound,
					this.getCouplerPosition(coupler), this.getVelocity(),
					1, 1, (int) (200 * gauge.scale()), soundScale(), SoundPacket.PacketSoundCategory.COUPLE)
					.sendToObserving(this);
		}

		switch (coupler) {
			case FRONT:
				coupledFront = id;
				break;
			case BACK:
				coupledBack = id;
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

	public Vec3d getCouplerPosition(CouplerType coupler) {
		SimulationState state = getCurrentState();
		if (state != null) {
			return coupler == CouplerType.FRONT ? state.couplerPositionFront : state.couplerPositionRear;
		}
		return getPosition();
	}

	/*
	 * Helpers
	 */
	
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

	@Override
	public boolean externalLightsEnabled() {
		return hasElectricalPower() && (
				gotElectricalPowerTick == -1 ||
						getTickCount() - gotElectricalPowerTick > 15 ||
						((getTickCount() - gotElectricalPowerTick)/(int)((Math.random()+2) * 4)) % 2 == 0
		);
	}
}
