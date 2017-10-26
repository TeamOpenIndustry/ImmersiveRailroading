package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.NBTUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
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

		public CouplerType opposite() {
			return this == FRONT ? BACK : FRONT;
		}
		
		public String toString() {
			return (this == FRONT ? ChatText.COUPLER_FRONT : ChatText.COUPLER_BACK).toString();
		}
	}

	public boolean isAttaching = false;

	private UUID coupledFront = null;
	private BlockPos lastKnownFront = null;
	private boolean frontCouplerEngaged = true;
	
	private UUID coupledBack = null;
	private BlockPos lastKnownRear= null;
	private boolean backCouplerEngaged = true;

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
		nbttagcompound.setBoolean("frontCouplerEngaged", frontCouplerEngaged);
		if (coupledBack != null) {
			nbttagcompound.setString("CoupledBack", coupledBack.toString());
			if (lastKnownRear != null) {
				nbttagcompound.setTag("lastKnownRear", NBTUtil.blockPosToNBT(lastKnownRear));
			}
		}
		nbttagcompound.setBoolean("backCouplerEngaged", backCouplerEngaged);
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
		frontCouplerEngaged = nbttagcompound.getBoolean("frontCouplerEngaged");

		if (nbttagcompound.hasKey("CoupledBack")) {
			coupledBack = UUID.fromString(nbttagcompound.getString("CoupledBack"));
			if (nbttagcompound.hasKey("lastKnownRear")) {
				lastKnownRear = NBTUtil.nbtToBlockPos(nbttagcompound.getCompoundTag("lastKnownRear"));
			}
		}
		backCouplerEngaged = nbttagcompound.getBoolean("backCouplerEngaged");
	}

	/*
	 * 
	 * Overrides
	 * 
	 */
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (player.getHeldItem(hand).getItem() == ImmersiveRailroading.ITEM_HOOK) {
			CouplerType coupler = CouplerType.FRONT;
			if (this.getCouplerPosition(CouplerType.FRONT).distanceTo(player.getPositionVector()) > this.getCouplerPosition(CouplerType.BACK).distanceTo(player.getPositionVector())) {
				coupler = CouplerType.BACK;
			}
			if (player.isSneaking()) {
				this.setCouplerEngaged(coupler, !this.isCouplerEngaged(coupler));
				if (this.isCouplerEngaged(coupler)) {
					player.sendMessage(ChatText.COUPLER_ENGAGED.getMessage(coupler));
				} else {
					player.sendMessage(ChatText.COUPLER_DISENGAGED.getMessage(coupler));
				}
			} else {
				if (this.isCoupled(coupler) && this.isCouplerEngaged(coupler)) {
					player.sendMessage(ChatText.COUPLER_STATUS_COUPLED.getMessage(coupler, this.getCoupled(coupler).getDefinition().name));
				} else {
					if (this.isCouplerEngaged(coupler)) {
						player.sendMessage(ChatText.COUPLER_STATUS_DECOUPLED_ENGAGED.getMessage(coupler));
					} else {
						player.sendMessage(ChatText.COUPLER_STATUS_DECOUPLED_DISENGAGED.getMessage(coupler));
					}
				}
			}
			return true;
		}
		return super.processInitialInteract(player, hand);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		

		if (world.isRemote) {
			// Only couple server side
			return;
		}
		
		if (this.getCurrentSpeed().minecraft() != 0) {
			ChunkManager.flagEntityPos(this.world, this.getPosition());
			if (this.lastKnownFront != null) {
				ChunkManager.flagEntityPos(this.world, this.lastKnownFront);
			}
			if (this.lastKnownRear != null) {
				ChunkManager.flagEntityPos(this.world, this.lastKnownRear);
			}
		}
		
		try {
			for (CouplerType coupler : CouplerType.values()) {
				if (this.ticksExisted > 20 && this.isCoupled(coupler) && this.getCoupled(coupler) == null) {
					System.out.println("Removing missing");
					this.decouple(coupler);
				}
				if (this.getCoupled(coupler) != null) {
					if (this.getCoupled(coupler).isDead) {
						System.out.println("Removing Dead Stock");
						this.decouple(coupler);
					} else {
						if (coupler == CouplerType.FRONT) {
							lastKnownFront = this.getCoupled(coupler).getPosition();
						} else {
							lastKnownRear = this.getCoupled(coupler).getPosition();
						}
					}
					
					if (!this.isCouplerEngaged(coupler)) {
						EntityCoupleableRollingStock otherStock = this.getCoupled(coupler);
						CouplerType otherCoupler = otherStock.getCouplerFor(this);
						if (otherCoupler == null) {
							System.out.println("MISSING COUPLER TOP");
							continue;
						}
						if (this.getCouplerPosition(coupler).distanceTo(otherStock.getCouplerPosition(otherCoupler)) > Config.couplerRange*4) {
							this.decouple(otherStock);
						}
					}
				}
			}
		} catch (Exception ex){
			ex.printStackTrace();
			ImmersiveRailroading.logger.error("Something broke in the decoupling code");
		}

		try {
			for (CouplerType coupler : CouplerType.values()) {
				if (!this.isCoupled(coupler)) {
					for (EntityCoupleableRollingStock potentialCoupling : this.potentialCouplings(coupler)) {
						for (CouplerType potentialCoupler : CouplerType.values()) {
							// Is the coupler free?
							if (!potentialCoupling.isCoupled(potentialCoupler)) {
								// Is the other coupler within coupling distance?
								
								for (EntityCoupleableRollingStock possiblyMe : potentialCoupling.potentialCouplings(potentialCoupler)) {
									if (possiblyMe.getPersistentID().equals(this.getPersistentID())) {
										this.setCoupledUUID(coupler, potentialCoupling.getPersistentID());
										potentialCoupling.setCoupledUUID(potentialCoupler, this.getPersistentID());
										
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
		} catch (Exception ex){
			ImmersiveRailroading.logger.error("Something broke in the coupling code");
			ex.printStackTrace();
		}
	}

	/*
	 * 
	 * Movement Handlers
	 * 
	 */

	public void simulateCoupledRollingStock() {
		this.sendToObserving(new MRSSyncPacket(this, this.positions));
		
		for (CouplerType coupler : CouplerType.values()) {
			EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
			if (coupled != null) {
				coupled.recursiveMove(this);
			}
		}
	}

	// This breaks with looped rolling stock
	private void recursiveMove(EntityCoupleableRollingStock prev) {
		// Clear out existing movement information
		TickPos lastPos = this.getCurrentTickPos();
		this.positions = new ArrayList<TickPos>();
		this.positions.add(lastPos);
		
		CouplerType coupler = this.getCouplerFor(prev);
		CouplerType otherCoupler = prev.getCouplerFor(this);
		
		if (coupler == null) {
			prev.decouple(this);
			this.decouple(prev);
			return;
		}

		if (!this.isCouplerEngaged(coupler) || !prev.isCouplerEngaged(otherCoupler)) {
			// Push only, no pull
			double prevDist = lastPos.position.distanceTo(prev.positions.get(0).position);
			double dist = lastPos.position.distanceTo(prev.positions.get(1).position);
			if (prevDist <= dist) {
				System.out.println("DETACHED");
				return;
			}
			System.out.println("ATTACHED");
		}
		
		for (TickPos parentPos : prev.positions) {
			
			Vec3d myOffset = this.getCouplerPositionTo(coupler, lastPos, parentPos);
			Vec3d otherOffset = prev.getCouplerPositionTo(otherCoupler, parentPos, lastPos);
			
			if (otherOffset == null) {
				if (!world.isRemote) {
					ImmersiveRailroading.logger.warn(String.format("Broken Coupling %s => %s", this.getPersistentID(), prev.getPersistentID()));
				}
				continue;
			}
			
			double distance = myOffset.distanceTo(otherOffset);

			// Figure out which direction to move the next stock
			Vec3d nextPosForward = myOffset.add(VecUtil.fromYaw(distance, lastPos.rotationYaw));
			Vec3d nextPosReverse = myOffset.add(VecUtil.fromYaw(-distance, lastPos.rotationYaw));

			if (otherOffset.distanceTo(nextPosForward) > otherOffset.distanceTo(nextPosReverse)) {
				// Moving in reverse
				distance = -distance;
			}

			lastPos = this.moveRollingStock(distance, lastPos.tickID);
			this.positions.add(lastPos);
			
			if (lastPos.speed.metric() != 0) {
				ChunkManager.flagEntityPos(this.world, new BlockPos(lastPos.position));
				for (CouplerType toChunk : CouplerType.values()) {
					ChunkManager.flagEntityPos(this.world, new BlockPos(this.getCouplerPosition(toChunk)));
				}
			}
		}
		this.sendToObserving(new MRSSyncPacket(this, this.positions));
		
		for (CouplerType nextCoupler : CouplerType.values()) {
			EntityCoupleableRollingStock coupled = this.getCoupled(nextCoupler);

			if (coupled == null || coupled == prev) {
				// Either end of train or wrong iteration direction
				continue;
			}
			
			coupled.recursiveMove(this);
		}
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
			lastKnownFront = null;
			break;
		case BACK:
			coupledBack = id;
			lastKnownRear = null;
			break;
		}
		triggerTrain();
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
		EntityCoupleableRollingStock stock = null;

		if (stock == null && this.getCoupledUUID(coupler) != null) {
			EntityCoupleableRollingStock coupledStockFront;
			EntityCoupleableRollingStock coupledStockBack;
			switch (coupler) {
			case FRONT:
				coupledStockFront = findByUUID(this.getCoupledUUID(coupler));
				return coupledStockFront;
			case BACK:
				coupledStockBack = findByUUID(this.getCoupledUUID(coupler));
				return coupledStockBack;
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
	
	public boolean isCouplerEngaged(CouplerType coupler) {
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
		case BACK:
			backCouplerEngaged = engaged;
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

	private Vec3d getCouplerPositionTo(CouplerType coupler, TickPos myPos, TickPos coupledPos) {
		//	Take the current position
		//	Add
		//		The Vector between the two couplers
		//		which has been normalized
		//  	then scaled to the distance between the stock position and the coupler
		//
		//	This works remarkably well even around corners
		return myPos.position.add(myPos.position.subtractReverse(coupledPos.position).normalize().scale(getDefinition().getCouplerPosition(coupler)));
	}

	public Vec3d getCouplerPosition(CouplerType coupler) {
		
		//Don't ask me why these are reversed...
		if (coupler == CouplerType.FRONT) {
			return predictRearBogeyPosition((float) (this.getDefinition().getLength()/2 + Config.couplerRange + this.getDefinition().getBogeyRear())).add(this.getPositionVector());
		} else {
			return predictFrontBogeyPosition((float) (this.getDefinition().getLength()/2 + Config.couplerRange - this.getDefinition().getBogeyFront())).add(this.getPositionVector());
		}
	}

	public List<EntityCoupleableRollingStock> potentialCouplings(CouplerType coupler) {
		
		double range = Config.couplerRange;
		
		List<EntityCoupleableRollingStock> nearBy = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, this.getCollisionBoundingBox().expand(range * 4, 0, 0));
		
		List<EntityCoupleableRollingStock> inRange = new ArrayList<EntityCoupleableRollingStock>();
		for (EntityCoupleableRollingStock stock : nearBy) {
			for (CouplerType otherCoupler : CouplerType.values()) {
				if (stock.isCoupled(otherCoupler)) {
					continue;
				}
				
				if (stock.getPersistentID().equals(this.getPersistentID())) {
					continue;
				}
				
				if (this.getCouplerPosition(coupler).distanceTo(stock.getCouplerPosition(otherCoupler)) < this.getCouplerPosition(coupler.opposite()).distanceTo(stock.getCouplerPosition(otherCoupler)) ) {
						inRange.add(stock);
				}
			}
		}
		
		List<EntityCoupleableRollingStock> toRemove = new ArrayList<EntityCoupleableRollingStock>();
		
		for (EntityCoupleableRollingStock stock : this.getTrain()) {
			// Prevent infinite loops
			for (EntityCoupleableRollingStock pot: inRange) {
				if (pot.getPersistentID().equals(stock.getPersistentID())) {
					toRemove.add(pot);
				}
			}
		}
		
		inRange.removeAll(toRemove);
		
		return inRange;
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
		return this.buildTrain(new ArrayList<EntityCoupleableRollingStock>(), followDisengaged);
	}

	private final List<EntityCoupleableRollingStock> buildTrain(List<EntityCoupleableRollingStock> train, boolean followDisengaged) {
		if (!train.contains(this)) {
			train.add(this);
			for (CouplerType coupler : CouplerType.values()) {
				if (this.getCoupled(coupler) != null) {
					boolean iAmCoupled = this.isCouplerEngaged(coupler);
					EntityCoupleableRollingStock other = this.getCoupled(coupler);
					CouplerType otherCoupler = other.getCouplerFor(this);
					if (otherCoupler == null) {
						System.out.println("MISSING COUPLER");
						continue;
					}
					boolean otherIsCoupled = other.isCouplerEngaged(otherCoupler); 
					if ((iAmCoupled && otherIsCoupled) || followDisengaged) {
						train = this.getCoupled(coupler).buildTrain(train, followDisengaged);
					}
				}
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
