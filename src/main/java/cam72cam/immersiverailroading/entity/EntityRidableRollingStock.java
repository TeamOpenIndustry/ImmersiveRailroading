package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock implements IRidable {
	private static final double SITTING_OFFSET = 0.6;

	public float getRidingSoundModifier() {
		return getDefinition().dampeningAmount;
	}

	@TagField(value = "payingPassengerPositions", mapper = PassengerMapper.class)
	private Map<UUID, Vec3d> payingPassengerPositions = new HashMap<>();

	@TagField(value = "seatedPassengers", mapper = SeatedMapper.class)
	@TagSync
	private Map<String, UUID> seatedPassengers = new HashMap<>();

	// Hack to remount players if they were seated
	private final Map<UUID, Vec3d> remount = new HashMap<>();

	// Floor position in the unrotated passenger frame. UMC stores the rotated
	// offset, so keeping this separate avoids feeding pitch/roll back into the
	// next tick's floor query.
	private final Map<UUID, Vec3d> standingFloorOffsets = new HashMap<>();



	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (player.isCrouching()) {
			return ClickResult.PASS;
		} else if (isPassenger(player)) {
			return ClickResult.PASS;
		} else {
			if (getWorld().isServer) {
				player.startRiding(this);
			}
			return ClickResult.ACCEPTED;
		}
	}

	private Vec3d getSeatOffset(UUID passenger) {
		String seat = seatedPassengers.entrySet().stream()
				.filter(x -> x.getValue().equals(passenger))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		return this.getDefinition().getModel().getSeats().stream()
		           .filter(s -> s.part.key.equals(seat))
		           .map(s -> {
		                   Vec3d seatPos = new Vec3d(s.part.center.z, s.part.min.y, -s.part.center.x).scale(gauge.scale());
		                   // The player stays upright, so the sitting height is world-vertical,
		                   // not along the rolled seat's local up axis.
		                   return getPassengerOrientation().apply(seatPos).subtract(0, SITTING_OFFSET, 0);
		           })
		           .findFirst().orElse(null);
	}

	@Override
	public Vec3d getMountOffset(Entity passenger, Vec3d off) {
		if (passenger.isVillager() && !payingPassengerPositions.containsKey(passenger.getUUID())) {
			payingPassengerPositions.put(passenger.getUUID(), passenger.getPosition());
		}

		if (passenger.isVillager() && !seatedPassengers.containsValue(passenger.getUUID())) {
			for (Seat<?> seat : getDefinition().getModel().getSeats()) {
				if (!seatedPassengers.containsKey(seat.part.key)) {
					seatedPassengers.put(seat.part.key, passenger.getUUID());
					break;
				}
			}
		}

		Vec3d seat = getSeatOffset(passenger.getUUID());
		if (seat != null) {
			standingFloorOffsets.remove(passenger.getUUID());
			return seat;
		}

		Matrix3 orientation = getPassengerOrientation();

		if (passenger.isVillager()) {
			int wiggle = 10;
			off = off.add((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		}
		off = this.getDefinition().correctPassengerBounds(gauge, off, shouldRiderSit(passenger), true);
		standingFloorOffsets.put(passenger.getUUID(), off);

		return orientation.apply(off);
	}

	@Override
	public boolean canFitPassenger(Entity passenger) {
		if (passenger instanceof Player && !((Player) passenger).hasPermission(Permissions.BOARD_STOCK)) {
			return false;
		}
		return getPassengerCount() < this.getDefinition().getMaxPassengers();
	}
	
	@Override
	public boolean shouldRiderSit(Entity passenger) {
		boolean nonSeated = this.getDefinition().shouldSit != null ? this.getDefinition().shouldSit : this.gauge.shouldSit();
		return nonSeated || this.seatedPassengers.containsValue(passenger.getUUID());
	}

	@Override
	public Vec3d onPassengerUpdate(Entity passenger, Vec3d offset) {
		Vec3d seat = getSeatOffset(passenger.getUUID());
		if (seat != null) {
			standingFloorOffsets.remove(passenger.getUUID());
			return seat;
		}

		Matrix3 orientation = getPassengerOrientation();
		UUID passengerId = passenger.getUUID();
		Vec3d floorOffset = standingFloorOffsets.get(passengerId);
		if (floorOffset == null) {
			// Only needed for offsets saved before the anchor was introduced.
			floorOffset = orientation.copy().transpose().apply(offset);
		}

		if (passenger.isPlayer()) {
			floorOffset = playerMovement(passenger.asPlayer(), floorOffset);
		}

		floorOffset = this.getDefinition().correctPassengerBounds(gauge, floorOffset, shouldRiderSit(passenger), false);
		standingFloorOffsets.put(passengerId, floorOffset);
		return orientation.apply(floorOffset);
	}

	private Matrix3 getPassengerOrientation() {
		// The model transform is roll then pitch after the -90 yaw flip, which
		// makes both angles negative in this entity-local frame.
		return Matrix3.fromEuler(0, -this.getRotationPitch(), -this.getRotationRoll());
	}

	private boolean isNearestConnectingDoorOpen(Player source) {
		// Find any doors that are close enough that are closed (and then negate)
		return !this.getDefinition().getModel().getDoors().stream()
		            .filter(d -> d.type == Door.Types.CONNECTING)
		            .filter(d -> d.center(this).distanceTo(source.getPosition()) < getDefinition().getLength(this.gauge)/3)
		            .min(Comparator.comparingDouble(d -> d.center(this).distanceTo(source.getPosition())))
		            .filter(x -> !x.isOpen(this))
		            .isPresent();
	}

	private Vec3d playerMovement(Player source, Vec3d offset) {
		Vec3d movement = source.getMovementInput();
        /*
        if (sprinting) {
            movement = movement.scale(3);
        }
        */
        if (movement.length() < 0.1) {
            return offset;
        }

        movement = new Vec3d(movement.x, 0, movement.z).rotateYaw(this.getRotationYaw() - source.getRotationYawHead());

		Vec3d other = getDefinition().calculateCorrectedMovement(this, this.gauge, offset, movement);
		offset = offset.add(other);

        if (this instanceof EntityCoupleableRollingStock couplable) {
            boolean atFront = this.getDefinition().isAtFront(gauge, offset);
			boolean atBack = this.getDefinition().isAtRear(gauge, offset);
			// TODO config for strict doors
			boolean atDoor = isNearestConnectingDoorOpen(source);

			atFront &= atDoor;
			atBack &= atDoor;

			for (CouplerType coupler : CouplerType.values()) {
				boolean atCoupler = coupler == CouplerType.FRONT ? atFront : atBack;
				if (atCoupler && couplable.isCoupled(coupler)) {
					EntityCoupleableRollingStock coupled = ((EntityCoupleableRollingStock) this).getCoupled(coupler);
					if (coupled != null) {
						if (((EntityRidableRollingStock)coupled).isNearestConnectingDoorOpen(source)) {
							coupled.addPassenger(source);
						}
					} else if (this.getTickCount() > 20) {
						ImmersiveRailroading.info(
								"Tried to move between cars (%s, %s), but %s was not found",
								this.getUUID(),
								couplable.getCoupledUUID(coupler),
								couplable.getCoupledUUID(coupler)
						);
					}
					return offset;
				}
			}
        }

        if (getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL))
			&& getWorld().isServer
			&& !this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(source), false).equals(offset)
		) {
        	this.removePassenger(source);
		}

		return offset;
	}

	@Override
	public void onTick() {
		super.onTick();

		if (getWorld().isServer && (isAddedToChunk() || Config.ConfigDebug.offlineVehicleLoadChunks)) {
			remount.forEach((uuid, pos) -> {
				Player player = getWorld().getEntity(uuid, Player.class);
				if (player != null) {
					player.setPosition(pos);
					player.startRiding(this);
				}
			});
			remount.clear();
			for (Player source : getWorld().getEntities(Player.class)) {
				if (source.getRiding() == null && getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL))) {
					this.addPassenger(source);
				}
			}
		}
	}

	public Vec3d onDismountPassenger(Entity passenger, Vec3d offset) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(passenger.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			if (getWorld().isServer && passenger.isPlayer()) {
				remount.put(passenger.getUUID(), passenger.getPosition());
			}
		}

		//TODO calculate better dismount offset
		Matrix3 orientation = getPassengerOrientation();
		Vec3d floorOffset = standingFloorOffsets.remove(passenger.getUUID());
		offset = floorOffset != null ? floorOffset : orientation.copy().transpose().apply(offset);
		offset = new Vec3d(Math.copySign(getDefinition().getWidth(gauge)/2 + 1, offset.x), 0, offset.z);

		if (getWorld().isServer && passenger.isVillager() && payingPassengerPositions.containsKey(passenger.getUUID())) {
			double distanceMoved = passenger.getPosition().distanceTo(payingPassengerPositions.get(passenger.getUUID()));

			int payout = (int) Math.floor(distanceMoved * Config.ConfigBalance.villagerPayoutPerMeter);

			List<ItemStack> payouts = Config.ConfigBalance.getVillagerPayout();
			if (payouts.size() != 0) {
				int type = (int)(Math.random() * 100) % payouts.size();
				ItemStack stack = payouts.get(type).copy();
				stack.setCount(payout);
				getWorld().dropItem(stack, getBlockPosition());
				// TODO drop by player or new pos?
			}
			payingPassengerPositions.remove(passenger.getUUID());
		}

		return orientation.apply(offset);
	}

	public void onSeatClick(String seat, Player player) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(player.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			return;
		}

		standingFloorOffsets.remove(player.getUUID());
		seatedPassengers.put(seat, player.getUUID());
	}

	private static class PassengerMapper implements TagMapper<Map<UUID, Vec3d>> {
		@Override
		public TagAccessor<Map<UUID, Vec3d>> apply(Class<Map<UUID, Vec3d>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, UUID::toString, (Vec3d pos) -> new TagCompound().setVec3d("pos", pos)),
					d -> d.getMap(fieldName, UUID::fromString, t -> t.getVec3d("pos"))
			);
		}
	}

	private static class SeatedMapper implements TagMapper<Map<String, UUID>> {
		@Override
		public TagAccessor<Map<String, UUID>> apply(Class<Map<String, UUID>> type, String fieldName, TagField tag) throws SerializationException {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, i -> i, u -> new TagCompound().setUUID("uuid", u)),
					d -> d.getMap(fieldName, i -> i, t -> t.getUUID("uuid"))
			);
		}
	}
}
