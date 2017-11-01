package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.NBTUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
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

	
	private boolean resimulate = false;
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
		
		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isCoupled(coupler)) {
				continue;
			}
			EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
			if (coupled == null) {
				if (this.ticksExisted > 20) {
					ImmersiveRailroading.warn("Coupled entity was not loaded after 20 ticks, decoupling");
					this.decouple(coupler);
				}
				continue;
			}
			if (coupled.isDead) {
				ImmersiveRailroading.warn("Removing Dead Stock");
				this.decouple(coupler);
				continue;
			}
			
			if (coupler == CouplerType.FRONT) {
				lastKnownFront = coupled.getPosition();
			} else {
				lastKnownRear = coupled.getPosition();
			}
			
			if (!this.isCouplerEngaged(coupler)) {
				CouplerType otherCoupler = coupled.getCouplerFor(this);
				if (otherCoupler == null) {
					continue;
				}
				if (this.getCouplerPosition(coupler).distanceTo(coupled.getCouplerPosition(otherCoupler)) > Config.couplerRange*4) {
					this.decouple(coupled);
				}
			}
		}

		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isCoupled(coupler)) {
				Pair<EntityCoupleableRollingStock, CouplerType> potential = this.potentialCouplings(coupler);
				if (potential == null) {
					continue;
				}
				
				EntityCoupleableRollingStock stock = potential.getLeft();
				CouplerType otherCoupler = potential.getRight();

				this.setCoupledUUID(coupler, stock.getPersistentID());
				stock.setCoupledUUID(otherCoupler, this.getPersistentID());
			}
		}
		
		
		if (world.isRemote) {
			return;
		}
		
		if (this.getRemainingPositions() < 20 || resimulate) {
			TickPos lastPos = this.getCurrentTickPosAndPrune();
			if (lastPos == null) {
				this.triggerResimulate();
				return;
			}
			
			Train train = this.getTrain();
			// Only simulate on locomotives if we can help it.
			if (!(this instanceof Locomotive)) {
				for (EntityCoupleableRollingStock stock : train) {
					if (stock instanceof Locomotive) {
						stock.resimulate = true;
						return;
					}
				}
			}
			
			boolean isStuck = false;
			for (EntityBuildableRollingStock stock : this.getTrain()) {
				if (!stock.areWheelsBuilt()) {
					isStuck = true;
				}
			}
			
			Speed simSpeed = this.getCurrentSpeed();
			if (isStuck) {
				simSpeed = Speed.ZERO;
			}
			
			// Clear out the list and re-simulate
			this.positions = new ArrayList<TickPos>();
			positions.add(lastPos);

			for (int i = 0; i < 30; i ++) {
				if (!isStuck) {
					simSpeed = train.getMovement(simSpeed, this.isReverse);
				}
				TickPos pos = this.moveRollingStock(simSpeed.minecraft(), lastPos.tickID + i);
				if (pos.speed.metric() != 0) {
					ChunkManager.flagEntityPos(this.world, new BlockPos(pos.position));
				}
				positions.add(pos);
			}
			
			simulateCoupledRollingStock();
			
			for (EntityCoupleableRollingStock stock : train) {
				stock.resimulate = false;
			}
		}
	}

	/*
	 * 
	 * Movement Handlers
	 * 
	 */

	public void simulateCoupledRollingStock() {
		for (int tickOffset = 1; tickOffset < this.positions.size(); tickOffset++) {
			boolean onTrack = true;
			for (CouplerType coupler : CouplerType.values()) {
				EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
				if (coupled != null) {
					onTrack = coupled.recursiveMove(this, tickOffset) && onTrack;
				}
			}
			if (!onTrack) {
				for (int i = tickOffset; i < this.positions.size(); i ++) {
					this.positions.get(i).position = this.positions.get(tickOffset).position;
					this.positions.get(i).speed = Speed.ZERO;
				}
				for (EntityCoupleableRollingStock entity : this.getTrain(true)) {
					entity.positions.get(entity.positions.size()-1).speed = Speed.ZERO;
				}
				break;
			}
		}
		for (EntityCoupleableRollingStock entity : this.getTrain(true)) {
			entity.sendToObserving(new MRSSyncPacket(entity, entity.positions));			
		}
	}

	// This breaks with looped rolling stock
	private boolean recursiveMove(EntityCoupleableRollingStock parent, int tickOffset) {
		if (this.positions.size() < tickOffset) {
			ImmersiveRailroading.warn("MISSING START POS " + tickOffset);
			return true;
		}
		
		TickPos currentPos = this.positions.get(tickOffset-1);
		TickPos parentPos = parent.positions.get(tickOffset);
		boolean onTrack = !currentPos.isOffTrack;
		
		if (tickOffset == 1) {
			// Clear out existing movement information
			this.positions = new ArrayList<TickPos>();
			this.positions.add(currentPos);
		}
		
		CouplerType coupler = this.getCouplerFor(parent);
		CouplerType otherCoupler = parent.getCouplerFor(this);
		
		if (coupler == null) {
			parent.decouple(this);
			this.decouple(parent);
			ImmersiveRailroading.warn("COUPLER NULL");
			return true;
		}
		
		boolean skipCalc = false;
		
		if ((!this.isCouplerEngaged(coupler) || !parent.isCouplerEngaged(otherCoupler))) {
			if (parent.positions.size() >= 2) {
				// Push only, no pull
				double prevDist = currentPos.position.distanceTo(parent.positions.get(tickOffset-1).position);
				double dist = currentPos.position.distanceTo(parent.positions.get(tickOffset).position);
				if (prevDist <= dist) {
					this.positions.add(this.moveRollingStock(this.getTrain(false).getMovement(currentPos.speed, currentPos.isReverse).minecraft(), currentPos.tickID));;
					skipCalc = true;
				}
			}
		}
		
		if (!skipCalc) {
		
		Vec3d myOffset = this.getCouplerPositionTo(coupler, currentPos, parentPos);
		Vec3d otherOffset = parent.getCouplerPositionTo(otherCoupler, parentPos, currentPos);
		
		if (otherOffset == null) {
			if (!world.isRemote) {
				ImmersiveRailroading.warn(String.format("Broken Coupling %s => %s", this.getPersistentID(), parent.getPersistentID()));
			}
			return true;
		}
		
		double distance = myOffset.distanceTo(otherOffset);

		// Figure out which direction to move the next stock
		Vec3d nextPosForward = myOffset.add(VecUtil.fromYaw(distance, currentPos.rotationYaw));
		Vec3d nextPosReverse = myOffset.add(VecUtil.fromYaw(-distance, currentPos.rotationYaw));

		if (otherOffset.distanceTo(nextPosForward) > otherOffset.distanceTo(nextPosReverse)) {
			// Moving in reverse
			distance = -distance;
		}

		
		TickPos nextPos = this.moveRollingStock(distance, currentPos.tickID);
		this.positions.add(nextPos);
		if (nextPos.isOffTrack) {
			onTrack = false;
		}
		
		if (nextPos.speed.metric() != 0) {
			ChunkManager.flagEntityPos(this.world, new BlockPos(nextPos.position));
			for (CouplerType toChunk : CouplerType.values()) {
				ChunkManager.flagEntityPos(this.world, new BlockPos(this.getCouplerPosition(toChunk, nextPos)));
			}
		}
		}
		
		for (CouplerType nextCoupler : CouplerType.values()) {
			EntityCoupleableRollingStock coupled = this.getCoupled(nextCoupler);

			if (coupled == null || coupled == parent) {
				// Either end of train or wrong iteration direction
				continue;
			}
			
			onTrack = coupled.recursiveMove(this, tickOffset) ? onTrack : false;
		}

		return onTrack;
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
		
		if (this.getCoupled(coupler) != null) {
			this.getTrain().smoothSpeeds();
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
		
		ImmersiveRailroading.info(this.getPersistentID() + " decouple " + coupler);

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
		return getCouplerPosition(coupler, this.getCurrentTickPosOrFake());
	}

	public Vec3d getCouplerPosition(CouplerType coupler, TickPos pos) {
		
		//Don't ask me why these are reversed...
		if (coupler == CouplerType.FRONT) {
			return predictRearBogeyPosition(pos, (float) (this.getDefinition().getLength()/2 + Config.couplerRange + this.getDefinition().getBogeyRear())).add(pos.position).addVector(0, 1, 0);
		} else {
			return predictFrontBogeyPosition(pos, (float) (this.getDefinition().getLength()/2 + Config.couplerRange - this.getDefinition().getBogeyFront())).add(pos.position).addVector(0, 1, 0);
		}
	}

	public Pair<EntityCoupleableRollingStock, CouplerType> potentialCouplings(CouplerType coupler) {
		List<EntityCoupleableRollingStock> train = this.getTrain();
		
		List<EntityCoupleableRollingStock> nearBy = world.getEntities(EntityCoupleableRollingStock.class, new Predicate<EntityCoupleableRollingStock>()
	    {
	        public boolean apply(@Nullable EntityCoupleableRollingStock entity)
	        {
	        	if (entity == null) {
	        		return false;
	        	}
	        	
	        	if (entity.isDead) {
	        		return false;
	        	}
	        	
	        	if (entity.getDistanceToEntity(EntityCoupleableRollingStock.this) > 64) {
	        		return false;
	        	}
	        	
	        	for (EntityCoupleableRollingStock stock : train) {
	        		if (stock.getUniqueID().equals(entity.getUniqueID())) {
	        			return false;
	        		}
	        	}
	        	
	            return true;
	        }
	    });
		
		Pair<EntityCoupleableRollingStock, CouplerType> bestMatch = null;
		double bestDistance = 100;
		
		
		/*
		 * 1. |-----+-----| |-----+-----|
		 * 2. |-----+---|=|----+-----|
		 * 3. |---|=+====+|-----|
		 */

		// getCouplerPosition is a somewhat expensive call, minimize if possible
		Vec3d myCouplerPos = this.getCouplerPosition(coupler);
		
		for (EntityCoupleableRollingStock stock : nearBy) {
			Vec3d stockFrontPos = stock.getCouplerPosition(CouplerType.FRONT);
			Vec3d stockBackPos = stock.getCouplerPosition(CouplerType.BACK);
			
			double couplerDistFront = this.getPositionVector().distanceTo(stockFrontPos);
			double couplerDistRear = this.getPositionVector().distanceTo(stockBackPos);
			
			// See above diagram (3).  OtherCoupler closet to my center is the one we want to couple to.
			CouplerType otherCoupler = couplerDistFront < couplerDistRear ? CouplerType.FRONT : CouplerType.BACK;
			if (stock.isCoupled(otherCoupler)) {
				//Best matching coupler is a no-go
				continue;
			}
			
			Vec3d stockCouplerPos = otherCoupler == CouplerType.FRONT ? stockFrontPos : stockBackPos;
			
			double myCouplerToOtherCoupler = myCouplerPos.distanceTo(stockCouplerPos);
			double myCenterToMyCoupler = this.getPositionVector().distanceTo(myCouplerPos);
			double myCenterToOtherCoupler = this.getPositionVector().distanceTo(stockCouplerPos);

			if (myCouplerToOtherCoupler > bestDistance) {
				// Current best match is closer, should be a small edge case when stock is almost entirely overlapping
				continue;
			}
			
			if (myCenterToMyCoupler < myCenterToOtherCoupler && this.isCouplerEngaged(coupler) && stock.isCouplerEngaged(otherCoupler)) {
				// diagram 1, check that it is not too far away
				if (myCouplerToOtherCoupler > Config.couplerRange) {
					// Not close enough to consider
					continue;
				}
			} else {
				// diagram 2 or diagram 3
				AxisAlignedBB myBB = this.getCollisionBoundingBox().contract(0, 0, 0.25); // Prevent overlap on other rails
				if (!myBB.contains(stockCouplerPos)) {
					continue;
				}
			}
			
			// findByUUID seems to work around a memcpy issue where refs are not updated
			stock = this.findByUUID(stock.getUniqueID());
			if (stock == null) {
				//this should not happen...
				continue;
			}
			
			bestMatch = Pair.of(stock, otherCoupler);
			bestDistance = myCouplerToOtherCoupler;
		}
		
		return bestMatch;
	}

	/*
	 * Helpers
	 */
	
	public void triggerTrain() {
		for (EntityCoupleableRollingStock stock : this.getTrain()) {
			stock.triggerResimulate();
		}
	}

	public final Train getTrain() {
		return getTrain(true);
	}

	public final Train getTrain(boolean followDisengaged) {
		return this.buildTrain(new Train(), followDisengaged);
	}

	private final Train buildTrain(Train train, boolean followDisengaged) {
		if (!train.contains(this)) {
			train.add(this);
			for (CouplerType coupler : CouplerType.values()) {
				if (this.getCoupled(coupler) != null) {
					boolean iAmCoupled = this.isCouplerEngaged(coupler);
					EntityCoupleableRollingStock other = this.getCoupled(coupler);
					CouplerType otherCoupler = other.getCouplerFor(this);
					if (otherCoupler == null) {
						//this.decouple(coupler);
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
	
	@Override
	public void triggerResimulate() {
		resimulate = true;
	}
}
