package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.CoupleStatusPacket;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.NBTUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.BlockPos;
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
	private BlockPos lastKnownFront = null;
	private int coupledFrontMissingTick = -1;
	private EntityCoupleableRollingStock coupledStockFront;
	
	private UUID coupledBack = null;
	private BlockPos lastKnownRear= null;
	private int coupledBackMissingTick = -1;
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
			if (lastKnownFront != null) {
				nbttagcompound.setTag("lastKnownFront", NBTUtil.blockPosToNBT(lastKnownFront));
			}
		}
		if (coupledBack != null) {
			nbttagcompound.setString("CoupledBack", coupledBack.toString());
			if (lastKnownRear != null) {
				nbttagcompound.setTag("lastKnownRear", NBTUtil.blockPosToNBT(lastKnownRear));
			}
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		if (nbttagcompound.hasKey("CoupledFront")) {
			coupledFront = UUID.fromString(nbttagcompound.getString("CoupledFront"));
			if (nbttagcompound.hasKey("lastKnownFront")) {
				lastKnownFront = NBTUtil.nbtToBlockPos(nbttagcompound.getCompoundTag("lastKnownFront"));
			}
		}

		if (nbttagcompound.hasKey("CoupledBack")) {
			coupledBack = UUID.fromString(nbttagcompound.getString("CoupledBack"));
			if (nbttagcompound.hasKey("lastKnownRear")) {
				lastKnownRear = NBTUtil.nbtToBlockPos(nbttagcompound.getCompoundTag("lastKnownRear"));
			}
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
		
		if (this.ticksExisted % 20 == 0) {
			this.sendToObserving(new CoupleStatusPacket(this));
		}

		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isCoupled(coupler)) {
				for (EntityCoupleableRollingStock potentialCoupling : this.potentialCouplings(coupler)) {
					for (CouplerType potentialCoupler : CouplerType.values()) {
						// Is the coupler free?
						if (!potentialCoupling.isCoupled(potentialCoupler)) {
							// Is the other coupler within coupling distance?
							for (EntityCoupleableRollingStock possiblyMe : potentialCoupling.potentialCouplings(potentialCoupler)) {
								if (possiblyMe.getPersistentID().equals(this.getPersistentID())) {
									
									this.setCoupled(coupler, potentialCoupling);
									potentialCoupling.setCoupled(potentialCoupler, this);
									
									this.sendToObserving(new CoupleStatusPacket(this));
									potentialCoupling.sendToObserving(new CoupleStatusPacket(potentialCoupling));
									
									break;
								}
							}
							if (this.isCoupled(coupler)) {
								// coupled
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
							.info(String.format("MISS %s %s %s", coupler, this.getDefinition().name, potentialCoupling.getDefinition().name));
				}
			}
		}
	}

	@Override
	public void setDead() {
		if (!world.isRemote) {
			try {
				throw new Exception("DECOUPLE");
			} catch (Exception e) {
				ImmersiveRailroading.logger.catching(e);
			}
			this.decouple();
		}
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
			recursiveMove(null, 0);
		}
	}

	// This breaks with looped rolling stock
	private void recursiveMove(EntityCoupleableRollingStock prev, int depth) {
		ChunkManager.flagEntityPos(this);
		
		if (depth > Config.maxTrainLength) {
			ImmersiveRailroading.logger.warn("TRAIN TOO LONG!");
			return;
		}
		
		for (CouplerType coupler : CouplerType.values()) {
			EntityCoupleableRollingStock coupled = this.getCoupled(coupler);

			if (coupled == null || coupled == prev) {
				// Either end of train or wrong iteration direction
				continue;
			}
			
			if (coupled == this) {
				this.decouple(coupler);
			}
			
			// THIS UPDATES LAST KNOWN POS
			setCoupled(coupler, coupled);
			
			Vec3d myOffset = this.getCouplerPositionTo(coupler, coupled);

			Vec3d otherOffset = null;
			for (CouplerType otherCoupler : CouplerType.values()) {
				EntityCoupleableRollingStock otherStock = coupled.getCoupled(otherCoupler);
				if (otherStock != null && otherStock.getPersistentID().equals(this.getPersistentID())) {
					// Matching coupler pair
					otherOffset = coupled.getCouplerPositionTo(otherCoupler, this);
				}
			}
			if (otherOffset == null) {
				if (!world.isRemote) {
					ImmersiveRailroading.logger.warn(String.format("Broken Coupling %s => %s", this.getPersistentID(), coupled.getPersistentID()));
					
					switch(coupler) {
					case FRONT:
						if (coupledFrontMissingTick == -1) {
							coupledFrontMissingTick = this.ticksExisted;
						} else if (coupledFrontMissingTick + 60 < this.ticksExisted) {
							// If we still have not seen the stock within 3 seconds, disconnect.
							this.decouple(coupler);
						}
						break;
					case BACK:
						if (coupledBackMissingTick == -1) {
							coupledBackMissingTick = this.ticksExisted;
						} else if (coupledBackMissingTick + 60 < this.ticksExisted) {
							// If we still have not seen the stock within 3 seconds, disconnect.
							this.decouple(coupler);
						}
						break;
					}
					// Some more debug because this randomly fails
					System.out.println(coupled == findByUUID(coupled.getPersistentID()));
					System.out.println(coupled.getCoupled(CouplerType.FRONT));
					System.out.println(coupled.getCoupledUUID(CouplerType.FRONT));
					System.out.println(coupled.getCoupled(CouplerType.BACK));
					System.out.println(coupled.getCoupledUUID(CouplerType.BACK));
				}
				continue;
			} else {
				switch(coupler) {
				case FRONT:
					coupledFrontMissingTick = -1;
				case BACK:
					coupledBackMissingTick = -1;
					break;
				}
			}

			double distance = myOffset.distanceTo(otherOffset);

			// Figure out which direction to move the next stock
			Vec3d nextPosForward = otherOffset.add(VecUtil.fromYaw(distance, coupled.rotationYaw));
			Vec3d nextPosReverse = otherOffset.add(VecUtil.fromYaw(-distance, coupled.rotationYaw));

			if (myOffset.distanceTo(nextPosForward) > myOffset.distanceTo(nextPosReverse)) {
				// Moving in reverse
				distance = -distance;
			}

			coupled.moveRollingStock(distance);
			coupled.recursiveMove(this, depth+1);
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
		if (id != null && id.equals(getCoupledUUID(coupler))) {
			// NOP
			return;
		}
		switch (coupler) {
		case FRONT:
			coupledFront = id;
			lastKnownFront = null;
			coupledStockFront = null;
			break;
		case BACK:
			coupledBack = id;
			lastKnownRear = null;
			coupledStockBack = null;
			break;
		}
	}

	public final void setCoupled(CouplerType coupler, EntityCoupleableRollingStock stock) {
		
		UUID id = null;
		BlockPos lastKnown = null;
		if (stock != null) {
			id = stock.getPersistentID();
			lastKnown = stock.getPosition();
		}
		
		switch (coupler) {
		case FRONT:
			coupledFront = id;
			lastKnownFront = lastKnown;
			coupledStockFront = stock;
			break;
		case BACK:
			coupledBack = id;
			lastKnownRear = lastKnown;
			coupledStockBack = stock;
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
				if (this.coupledStockFront == null) {
					ChunkManager.flagEntityPos(this.world, this.lastKnownFront);
				}
				return this.coupledStockFront;
			case BACK:
				this.coupledStockBack = findByUUID(this.getCoupledUUID(coupler));
				if (this.coupledStockBack == null) {
					ChunkManager.flagEntityPos(this.world, this.lastKnownRear);
				}
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
		
		System.out.println(this.getPersistentID() + " decouple " + coupler);

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

	private Vec3d getCouplerPositionTo(CouplerType coupler, EntityCoupleableRollingStock coupled) {
		//	Take the current position
		//	Add
		//		The Vector between the two couplers
		//		which has been normalized
		//  	then scaled to the distance between the stock position and the coupler
		//
		//	This works remarkably well even around corners
		return this.getPositionVector().add(this.getPositionVector().subtractReverse(coupled.getPositionVector()).normalize().scale(getDefinition().getCouplerPosition(coupler)));
	}

	public Vec3d getCouplerPosition(CouplerType coupler) {
		return VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler), rotationYaw + coupler.yaw).add(getPositionVector());
	}

	public List<EntityCoupleableRollingStock> potentialCouplings(CouplerType coupler) {
		Vec3d pos = VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler) + Config.couplerRange, rotationYaw + coupler.yaw).add(getPositionVector());
		
		double range = Config.couplerRange;
		
		List<EntityCoupleableRollingStock> nearBy = world.getEntities(EntityCoupleableRollingStock.class, EntitySelectors.withinRange(pos.x, pos.y, pos.z, 32));;
		nearBy.remove(this); // just to be safe
		
		List<EntityCoupleableRollingStock> inRange = new ArrayList<EntityCoupleableRollingStock>();
		for (EntityCoupleableRollingStock stock : nearBy) {
			for (CouplerType otherCoupler : CouplerType.values()) {
				if (this.getCouplerPosition(coupler).subtract(stock.getCouplerPosition(otherCoupler)).lengthVector() < range) {
					inRange.add(stock);
				}
			}
		}
		
		for (EntityCoupleableRollingStock stock : this.getTrain()) {
			// Prevent infinite loops
			inRange.remove(stock);
		}
		
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
		//List<EntityCoupleableRollingStock> elist = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, this.getCollisionBoundingBox().grow(ImmersiveRailroading.ENTITY_SYNC_DISTANCE));
		//for (Object e : elist) {
		for (Object e : world.loadedEntityList) {
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
