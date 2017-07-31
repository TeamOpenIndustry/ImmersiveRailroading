package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.CoupleStatusPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityCoupleableRollingStock extends EntityMoveableRollingStock {

	public enum CouplerType {
		FRONT(0), BACK(180);

		public final float yaw;

		CouplerType(float yaw) {
			this.yaw = yaw;
		}
	}

	public boolean isAttaching = false;

	private UUID coupledFront = null;
	private UUID coupledBack = null;
	private EntityCoupleableRollingStock coupledStockFront;
	private EntityCoupleableRollingStock coupledStockBack;

	public EntityCoupleableRollingStock(World world, String defID) {
		super(world, defID);
	}

	/*
	 * 
	 * Data Read/Write
	 * 
	 */

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		coupledFront = BufferUtil.readUUID(additionalData);
		coupledBack = BufferUtil.readUUID(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		BufferUtil.writeUUID(buffer, coupledFront);
		BufferUtil.writeUUID(buffer, coupledBack);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		if (coupledFront != null) {
			nbttagcompound.setString("CoupledFront", coupledFront.toString());
		}
		if (coupledBack != null) {
			nbttagcompound.setString("CoupledBack", coupledBack.toString());
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		if (nbttagcompound.hasKey("CoupledFront")) {
			coupledFront = UUID.fromString(nbttagcompound.getString("CoupledFront"));
		}

		if (nbttagcompound.hasKey("CoupledBack")) {
			coupledBack = UUID.fromString(nbttagcompound.getString("CoupledBack"));
		}
	}

	/*
	 * 
	 * Overrides
	 * 
	 */

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			// Only couple server side
			return;
		}

		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isCoupled(coupler)) {
				for (EntityCoupleableRollingStock potentialCoupling : this.potentialCouplings(coupler)) {
					for (CouplerType potentialCoupler : CouplerType.values()) {
						// Is the coupler free?
						if (!potentialCoupling.isCoupled(potentialCoupler)) {
							// Is the other coupler within coupling distance?
							if (potentialCoupling.potentialCouplings(potentialCoupler).contains(this)) {
								this.setCoupledUUID(coupler, potentialCoupling.getPersistentID());
								potentialCoupling.setCoupledUUID(potentialCoupler, this.getPersistentID());
								this.sendToObserving(new CoupleStatusPacket(this));
								potentialCoupling.sendToObserving(new CoupleStatusPacket(potentialCoupling));
								break;
							}
						}
					}

					if (this.isCoupled(coupler)) {
						// coupled
						break;
					}

					// False Match
					ImmersiveRailroading.logger
							.info(String.format("MISS %s %s %s", coupler, this.getPersistentID(), potentialCoupling.getPersistentID()));
				}
			}
		}
	}

	@Override
	public void setDead() {
		this.decouple();
		super.setDead();
	}

	/*
	 * 
	 * Movement Handlers
	 * 
	 */

	public void moveCoupledRollingStock(Float moveDistance) {
		this.moveRollingStock(moveDistance);
		if (Math.abs(moveDistance) > 0.01) {
			recursiveMove(null);
		}
	}

	// This breaks with looped rolling stock
	// TODO prevent looped trains
	private void recursiveMove(EntityCoupleableRollingStock prev) {
		for (CouplerType coupler : CouplerType.values()) {
			EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
			Vec3d myOffset = this.getCouplerPosition(coupler);

			if (coupled == null || coupled == prev) {
				// Either end of train or wrong iteration direction
				continue;
			}

			Vec3d otherOffset = null;
			for (CouplerType otherCoupler : CouplerType.values()) {
				if (coupled.getCoupled(otherCoupler) == this) {
					// Matching coupler pair
					otherOffset = coupled.getCouplerPosition(otherCoupler);
				}
			}
			if (otherOffset == null) {
				ImmersiveRailroading.logger.warn("Broken Coupling %s => %s", this.getPersistentID(), coupled.getPersistentID());
				continue;
			}

			double distance = myOffset.distanceTo(otherOffset);

			// TEMP
			if (distance > 1) {
				// ImmersiveRailroading.logger.warn(String.format("%s too far
				// from %s", this.getPersistentID(),
				// coupled.getPersistentID()));
				// ImmersiveRailroading.logger.warn(String.format("%s --> %s",
				// myOffset, otherOffset));
				// return;
			}

			// Figure out which direction to move the next stock
			Vec3d nextPosForward = otherOffset.add(VecUtil.fromYaw(distance, coupled.rotationYaw));
			Vec3d nextPosReverse = otherOffset.add(VecUtil.fromYaw(-distance, coupled.rotationYaw));

			if (myOffset.distanceTo(nextPosForward) > myOffset.distanceTo(nextPosReverse)) {
				// Moving in reverse
				distance = -distance;
			}

			coupled.moveRollingStock(distance);
			coupled.recursiveMove(this);
		}
	}

	/*
	 * Coupler Getters and Setters
	 * 
	 */

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

	public final void setCoupledUUID(CouplerType coupler, UUID id) {
		if (this.getCoupledUUID(coupler) != null && this.getCoupledUUID(coupler).equals(id)) {
			return;
		}
		switch (coupler) {
		case FRONT:
			coupledFront = id;
			coupledStockFront = null;
			break;
		case BACK:
			coupledBack = id;
			coupledStockBack = null;
			break;
		}
	}

	public EntityCoupleableRollingStock getCoupled(CouplerType coupler) {
		EntityCoupleableRollingStock stock = null;

		switch (coupler) {
		case FRONT:
			stock = this.coupledStockFront;
			break;
		case BACK:
			stock = this.coupledStockBack;
			break;
		}

		if (stock == null && this.getCoupledUUID(coupler) != null) {
			switch (coupler) {
			case FRONT:
				this.coupledStockFront = findByUUID(this.getCoupledUUID(coupler));
				return this.coupledStockFront;
			case BACK:
				this.coupledStockBack = findByUUID(this.getCoupledUUID(coupler));
				return this.coupledStockBack;
			}
		}
		return stock;
	}

	public CouplerType getCouplerFor(EntityCoupleableRollingStock stock) {
		for (CouplerType coupler : CouplerType.values()) {
			if (this.getCoupled(coupler) == stock) {
				return coupler;
			}
		}
		return null;
	}

	/*
	 * Checkers
	 * 
	 */

	public final boolean isCoupled() {
		return isCoupled(CouplerType.FRONT) && isCoupled(CouplerType.BACK);
	}
	
	public boolean isCoupled(EntityCoupleableRollingStock stock) {
		return this.getCoupled(CouplerType.FRONT) == stock || this.getCoupled(CouplerType.BACK) == stock;
	}

	public final boolean isCoupled(CouplerType coupler) {
		return getCoupledUUID(coupler) != null;
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
		if (stock.getPersistentID().equals(this.getCoupledUUID(CouplerType.FRONT))) {
			decouple(CouplerType.FRONT);
		} else if (stock.getPersistentID().equals(this.getCoupledUUID(CouplerType.BACK))) {
			decouple(CouplerType.BACK);
		}
	}

	public void decouple(CouplerType coupler) {
		EntityCoupleableRollingStock coupled = getCoupled(coupler);

		// Break the coupling
		this.setCoupledUUID(coupler, null);

		// Ask the connected car to do the same
		if (coupled != null) {
			coupled.decouple(this);
		}
	}

	/*
	 * Get cars by coupled bounding boxes
	 */

	public Vec3d getCouplerPosition(CouplerType coupler) {
		return VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler), rotationYaw + coupler.yaw).add(getPositionVector());
	}

	public List<EntityCoupleableRollingStock> potentialCouplings(CouplerType coupler) {
		return getInCouplerRange(VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler) + Config.couplerRange, rotationYaw + coupler.yaw)
				.add(getPositionVector()));
	}

	private List<EntityCoupleableRollingStock> getInCouplerRange(Vec3d pos) {
		double range = Config.couplerRange;
		AxisAlignedBB bb = new AxisAlignedBB(-range, -range, -range, range, range, range);
		List<EntityCoupleableRollingStock> inRange = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, bb.offset(pos).offset(0, 1, 0));
		inRange.remove(this); // just to be safe
		return inRange;
	}

	/*
	 * Helpers
	 */

	public final List<EntityCoupleableRollingStock> getTrain() {
		return this.buildTrain(new ArrayList<EntityCoupleableRollingStock>());
	}

	private final List<EntityCoupleableRollingStock> buildTrain(List<EntityCoupleableRollingStock> train) {
		if (!train.contains(this)) {
			train.add(this);
			if (this.getCoupled(CouplerType.FRONT) != null) {
				train = this.getCoupled(CouplerType.FRONT).buildTrain(train);
			}
			if (this.getCoupled(CouplerType.BACK) != null) {
				train = this.getCoupled(CouplerType.BACK).buildTrain(train);
			}
		}
		return train;
	}

	public EntityCoupleableRollingStock findByUUID(UUID uuid) {
		// May want to cache this if it happens a lot
		List<EntityCoupleableRollingStock> elist = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, this.getCollisionBoundingBox().grow(ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
		for (Object e : elist) {
			if (e instanceof EntityCoupleableRollingStock) {
				EntityCoupleableRollingStock train = (EntityCoupleableRollingStock) e;
				if (train.getPersistentID().equals(uuid)) {
					return train;
				}
			}
		}
		return null;
	}
}
