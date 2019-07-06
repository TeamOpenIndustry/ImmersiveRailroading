package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.Hand;

import java.util.List;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock implements IRidable {
	private static final double pressDist = 0.05;

	public EntityRidableRollingStock(ModdedEntity entity) {
		super(entity);
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
				addPassenger(player);
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
		off = off.add(0, -off.y, 0);
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

	public void handleKeyPress(Player source, KeyTypes key, boolean sprinting) {
		Vec3d movement;
		switch (key) {
		case PLAYER_FORWARD:
			movement = new Vec3d(pressDist, 0, 0);
			break;
		case PLAYER_BACKWARD:
			movement = new Vec3d(-pressDist, 0, 0);
			break;
		case PLAYER_LEFT:
			movement = new Vec3d(0, 0, -pressDist);
			break;
		case PLAYER_RIGHT:
			movement = new Vec3d(0, 0, pressDist);
			break;
		default:
			//ignore key
			return;
		}
		if (this.isPassenger(source)) {
			if (sprinting) {
				movement = movement.scale(3);
			}
			
			movement = VecUtil.rotateWrongYaw(movement, source.getYawHead());
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
			passenger.setPosition(pos);
			passenger.setVelocity(this.getVelocity());

			passenger.internal.prevRotationYaw = passenger.internal.rotationYaw;
			passenger.internal.rotationYaw += (this.getRotationYaw() - this.getPrevRotationYaw());
		}
	}

	public Vec3d getDismountPosition(Entity ent) {
		Vec3d pos = this.getDefinition().getPassengerCenter(gauge);
		pos = pos.add(this.getRidingOffset(ent));
		pos = VecUtil.rotateWrongYaw(pos, getRotationYaw());
		pos = pos.add(this.getPosition());

		Vec3d ppos = getRidingOffset(ent);
		Vec3d delta = VecUtil.fromWrongYaw(this.getDefinition().getPassengerCompartmentWidth(gauge)/2 + 1.3 * gauge.scale(), this.getRotationYaw() + (ppos.z > 0 ? 90 : -90));
		
		return delta.add(pos);
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
