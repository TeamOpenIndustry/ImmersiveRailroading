package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.Hand;

import java.util.List;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock implements IRidable {
	public float getRidingSoundModifier() {
		return getDefinition().dampeningAmount;
	}

	@Override
	public ClickResult onClick(Player player, Hand hand) {
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

	@Override
	public Vec3d getMountPosition(Entity entity) {
		Vec3d pos = entity.getPosition();
		Vec3d center = this.getDefinition().getPassengerCenter(gauge);
		center = VecUtil.rotateWrongYaw(center, super.getRotationYaw());
		center = center.add(this.getPosition());
		Vec3d off = VecUtil.rotateWrongYaw(center.subtract(pos), -this.getRotationYaw());

		off = this.getDefinition().correctPassengerBounds(gauge, off);
		int wiggle = entity.isVillager() ? 10 : 2;
		off = off.add((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		off = this.getDefinition().correctPassengerBounds(gauge, off);
		off = new Vec3d(off.x, this.getDefinition().getPassengerCenter(gauge).y, off.z);
		return off;
	}

	@Override
	public boolean canFitPassenger(Entity passenger) {
		return getPassengerCount() < this.getDefinition().getMaxPassengers();
	}
	
	@Override
	public boolean shouldRiderSit(Entity ent) {
		if (this.getDefinition().shouldSit != null) {
			return this.getDefinition().shouldSit;
		}
		return this.gauge.shouldSit();
	}

	@Override
	public void onTick() {
		super.onTick();
		if (getWorld().isClient) {
			return;
		}
		for (Entity passenger : this.getPassengers()) {
			if (passenger.isPlayer()) {
				playerMovement(passenger.asPlayer(), Keyboard.getMovement(passenger.asPlayer()));
			}
		}
	}
	private void playerMovement(Player source, Vec3d movement) {
		if (this.isPassenger(source)) {
			/*
			if (sprinting) {
				movement = movement.scale(3);
			}
			*/
			
			movement = VecUtil.rotateYaw(new Vec3d(movement.x, 0, -movement.z), -source.getYawHead());
			movement = VecUtil.rotateWrongYaw(movement, 180-this.getRotationYaw());

			Vec3d pos = getRidingOffset(source).add(movement);

			if (this instanceof EntityCoupleableRollingStock) {
				if (this.getDefinition().isAtFront(gauge, pos) && ((EntityCoupleableRollingStock)this).isCoupled(CouplerType.FRONT)) {
					((EntityCoupleableRollingStock)this).getCoupled(CouplerType.FRONT).addPassenger(source);
					return;
				}
				if (this.getDefinition().isAtRear(gauge, pos) && ((EntityCoupleableRollingStock)this).isCoupled(CouplerType.BACK)) {
					((EntityCoupleableRollingStock)this).getCoupled(CouplerType.BACK).addPassenger(source);
					return;
				}
			}
			
			pos = this.getDefinition().correctPassengerBounds(gauge, pos);
			setRidingOffset(source, pos);
		}
	}

	@Override
    public void updatePassenger(Entity passenger) {
		Vec3d ppos = getRidingOffset(passenger);
		if (ppos != null && this.isPassenger(passenger)) {
			Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
			pos = pos.add(ppos);
			pos = VecUtil.rotatePitch(pos, getRotationPitch());
			pos = VecUtil.rotateWrongYaw(pos, getRotationYaw());
			pos = pos.add(getPosition());
			if (shouldRiderSit(passenger)) {
				pos = pos.subtract(0, 0.75, 0);
			}
			passenger.setPosition(new Vec3d(pos.x, getPosition().y + getDefinition().getPassengerCenter(gauge).y, pos.z));
			passenger.setVelocity(this.getVelocity());

            passenger.setRotationYaw(passenger.getRotationYaw() + (this.getRotationYaw() - this.getPrevRotationYaw()));
		}
	}

	public Vec3d getDismountPosition(Entity ent) {
		Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
		pos = pos.add(this.getRidingOffset(ent));
		pos = VecUtil.rotateWrongYaw(pos, getRotationYaw());
		pos = pos.add(this.getPosition());

		Vec3d ppos = getRidingOffset(ent);
		Vec3d delta = VecUtil.fromWrongYaw(this.getDefinition().getPassengerCompartmentWidth(gauge)/2 + 1.3 * gauge.scale(), this.getRotationYaw() + (ppos.z > 0 ? 90 : -90));

		pos = delta.add(pos);

		return new Vec3d(pos.x, getPosition().y+1, pos.z);
	}

	@Override
	public void onDismountPassenger(Entity entity) {
		if (entity.isVillager()) {
			double distanceMoved = entity.getPosition().distanceTo(getPosition());

			int payout = (int) Math.floor(distanceMoved * Config.ConfigBalance.villagerPayoutPerMeter);

			List<ItemStack> payouts = Config.ConfigBalance.getVillagerPayout();
			if (payouts.size() != 0) {
				int type = (int)(Math.random() * 100) % payouts.size();
				ItemStack stack = payouts.get(type).copy();
				stack.setCount(payout);
				getWorld().dropItem(stack, getBlockPosition());
				// TODO drop by player or new pos?
			}
		}
	}
}
