package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import cam72cam.immersiverailroading.util.RealBB;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.physics.PhysicsAccummulator;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;

public abstract class EntityCoupleableRollingStock extends EntityMoveableRollingStock {
	static {
		World.onTick(world -> {
			if (world.isClient) {
				return;
			}
			// We do this here as to let all the entities do their onTick first.  Otherwise some might be one onTick ahead
			// if we did this in the onUpdate method
			List<EntityCoupleableRollingStock> entities = world.getEntities(EntityCoupleableRollingStock.class);

			for (EntityCoupleableRollingStock stock : entities) {
				if (stock.getCurrentSpeed().minecraft() != 0 || ConfigDebug.keepStockLoaded) {
					world.keepLoaded(stock.getBlockPosition());
				}
			}

			// Try locomotives first
			for (EntityCoupleableRollingStock stock : entities) {
				if (stock instanceof Locomotive) {
					//stock = stock.findByUUID(stock.getUUID());
					stock.tickPosRemainingCheck();
				}
			}
			// Try rest
			for (EntityCoupleableRollingStock stock : entities) {
				//stock = stock.findByUUID(stock.getUUID());
				stock.tickPosRemainingCheck();
			}

			try {
				Thread.sleep(ConfigDebug.lagServer);
			} catch (InterruptedException e) {
				ImmersiveRailroading.catching(e);
			}
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

	
	private boolean resimulate = false;
	private int resimulateCooldown = 0;
	public boolean isAttaching = false;

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

	/*
	 * 
	 * Overrides
	 * 
	 */
	
	@Override
    public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_HOOK) && getWorld().isServer) {
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
			
			return;
		}

		if (this.resimulateCooldown > 0) {
			this.resimulateCooldown -= 1;
		}

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
		
		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isCoupled(coupler)) {
				continue;
			}
			EntityCoupleableRollingStock coupled = this.getCoupled(coupler);
			if (coupled == null) {
				if (this.getTickCount() > 20) {
					ImmersiveRailroading.warn("Coupled entity was not loaded after 20 ticks, decoupling");
					this.decouple(coupler);
				}
				continue;
			}
			if (coupled.isDead()) {
				ImmersiveRailroading.warn("Removing Dead Stock");
				this.decouple(coupler);
				continue;
			}
			
			if (coupler == CouplerType.FRONT) {
				lastKnownFront = coupled.getBlockPosition();
			} else {
				lastKnownRear = coupled.getBlockPosition();
			}
			
			if (!this.isCouplerEngaged(coupler)) {
				CouplerType otherCoupler = coupled.getCouplerFor(this);
				if (otherCoupler == null) {
					continue;
				}
				if (this.getCouplerPosition(coupler).distanceTo(coupled.getCouplerPosition(otherCoupler)) > ConfigDebug.couplerRange*4) {
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
				this.setCoupledUUID(coupler, stock.getUUID());
				stock.setCoupledUUID(otherCoupler, this.getUUID());
				if (stock.isCouplerEngaged(otherCoupler) && this.isCouplerEngaged(coupler)) {
					new SoundPacket("immersiverailroading:sounds/default/coupling.ogg", this.getCouplerPosition(coupler), this.getVelocity(), 1, 1, 200, gauge)
						.sendToObserving(this);
				}
			}
		}
	}
	
	private Vec3d guessCouplerPosition(CouplerType coupler) {
		return getPosition().add(VecUtil.fromWrongYaw(this.getDefinition().getLength(gauge)/2 * (coupler == CouplerType.FRONT ? 1 : -1), this.getRotationYaw()));
	}

	public void tickPosRemainingCheck() {
		if (this.getRemainingPositions() < 10 || resimulate) {
			TickPos lastPos = this.getCurrentTickPosAndPrune();
			if (lastPos == null) {
				this.triggerResimulate();
				return;
			}
			
			// Only simulate on locomotives if we can help it.
			/*
			if (!(this instanceof Locomotive)) {
				for (EntityCoupleableRollingStock stock : this.getTrain()) {
					if (stock instanceof Locomotive) {
						stock.resimulate = true;
						return;
					}
				}
			}
			*/
			
			if (resimulate && this.getTickCount() % 5 != 0) {
				// Resimulate every 5 ticks, this will cut down on packet storms
				return;
			}
			/*
			
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
					simSpeed = this.getMovement(simSpeed);
				}
				TickPos pos = this.moveRollingStock(simSpeed.minecraft(), lastPos.tickID + i);
				if (pos.speed.metric() != 0) {
					ChunkManager.flagEntityPos(this.internal, new Vec3i(pos.position));
				}
				positions.add(pos);
			}*/
			
			simulateCoupledRollingStock();
		}
	}

	/*
	 * 
	 * Movement Handlers
	 * 
	 */

	private Speed getMovement(TickPos currentPos, boolean followStock) {
		PhysicsAccummulator acc = new PhysicsAccummulator(currentPos);
		this.mapTrain(this, true, followStock, acc::accumulate);
		return acc.getVelocity();
	}

	private Speed getMovement(TickPos currentPos, Collection<DirectionalStock> train) {
		PhysicsAccummulator acc = new PhysicsAccummulator(currentPos);
		for (DirectionalStock stock : train) {
			acc.accumulate(stock.stock, stock.direction);
		}
		return acc.getVelocity();
	}
	

	public void simulateCoupledRollingStock() {
		TickPos lastPos = this.getCurrentTickPosAndPrune();
		this.positions = new ArrayList<TickPos>();
		positions.add(lastPos);
		
		
		
		Collection<DirectionalStock> train = this.getDirectionalTrain(true);

		Speed simSpeed = this.getCurrentSpeed();
		boolean isStuck = false;
		for (DirectionalStock stock : train) {
			if (!stock.stock.areWheelsBuilt()) {
				isStuck = true;
			}
		}
		
		
		for (int tickOffset = 1; tickOffset < 30; tickOffset++) {
			simSpeed = this.getMovement(this.positions.get(tickOffset-1), train);
			if (isStuck) {
				simSpeed = Speed.ZERO;
			}
			TickPos pos = this.moveRollingStock(simSpeed.minecraft(), lastPos.tickID + tickOffset - 1);
			positions.add(pos);
			
			for (DirectionalStock stock : train) {
				if (stock.stock.getUUID().equals(this.getUUID())) {
					//Skip self
					continue;
				}
				isStuck &= !stock.stock.simulateMove(stock.prev, tickOffset);
			}
		}
		
		
		for (DirectionalStock entity : train) {
			new MRSSyncPacket(entity.stock, entity.stock.positions).sendToObserving(entity.stock);
			entity.stock.resimulate = false;
		}
	}

	// This breaks with looped rolling stock
	private boolean simulateMove(EntityCoupleableRollingStock parent, int tickOffset) {
		if (this.positions.size() < tickOffset) {
			ImmersiveRailroading.debug("MISSING START POS " + tickOffset);
			return true;
		}
		
		TickPos currentPos = this.positions.get(tickOffset-1);
		
		if (parent.positions.size() <= tickOffset) {
			//TODO better hack
			return currentPos.isOffTrack;
		}
		
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
			return onTrack;
		}
		
		if ((!this.isCouplerEngaged(coupler) || !parent.isCouplerEngaged(otherCoupler)) && this.getTickCount() > 5 && parent.getTickCount() > 5) {
			if (parent.positions.size() >= 2) {
				// Push only, no pull
				double prevDist = currentPos.position.distanceTo(parent.positions.get(tickOffset-1).position);
				double dist = currentPos.position.distanceTo(parent.positions.get(tickOffset).position);
				if (prevDist <= dist) {
					TickPos nextPos = this.moveRollingStock(getMovement(currentPos, false).minecraft(), currentPos.tickID);
					this.positions.add(nextPos);
					if (nextPos.isOffTrack) {
						onTrack = false;
					}
					return onTrack;
				}
			}
		}
		
		Vec3d myOffset = this.getCouplerPositionTo(coupler, currentPos, parentPos);
		Vec3d otherOffset = parent.getCouplerPositionTo(otherCoupler, parentPos, currentPos);
		
		if (otherOffset == null) {
			if (getWorld().isServer) {
				ImmersiveRailroading.warn(String.format("Broken Coupling %s => %s", this.getUUID(), parent.getUUID()));
			}
			return onTrack;
		}
		
		double distance = myOffset.distanceTo(otherOffset);

		if (distance - Math.abs(currentPos.speed.minecraft()) > gauge.scale() * 20) {
			ImmersiveRailroading.warn("Decoupling stock that are too far apart!");
			this.decouple();
			distance = Math.abs(parentPos.speed.minecraft());
		}

		// Figure out which direction to move the next stock
		Vec3d nextPosForward = myOffset.add(VecUtil.fromWrongYaw(distance, currentPos.rotationYaw));
		Vec3d nextPosReverse = myOffset.add(VecUtil.fromWrongYaw(-distance, currentPos.rotationYaw));

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
			getWorld().keepLoaded(new Vec3i(nextPos.position));
			for (CouplerType toChunk : CouplerType.values()) {
				getWorld().keepLoaded(new Vec3i(this.getCouplerPosition(toChunk, nextPos)));
			}
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
		
		if (this.getCoupled(coupler) != null) {
			BiConsumer<EntityCoupleableRollingStock, Boolean> fn = new BiConsumer<EntityCoupleableRollingStock, Boolean>() {
				double speed = 0;
				double weight = 0;
				
				@Override
				public void accept(EntityCoupleableRollingStock e, Boolean direction) {
					speed += e.getCurrentSpeed().metric() * e.getWeight() * (direction ? 1 : -1);
					weight += e.getWeight();
				}				
				@Override
				public int hashCode() {
					return (int) (speed / weight);
				}
			};
			this.mapTrain(this, true, true, fn);
			Speed speedPos = Speed.fromMetric(fn.hashCode());
			Speed speedNeg = Speed.fromMetric(-fn.hashCode());
			this.mapTrain(this, true, true, (EntityCoupleableRollingStock e, Boolean b) -> e.setCurrentSpeed(b ? speedPos : speedNeg));
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
			break;
		case BACK:
			backCouplerEngaged = engaged;
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

	/*
	 * Get cars by coupled bounding boxes
	 */

	private Vec3d getCouplerPositionTo(CouplerType coupler, TickPos myPos, TickPos coupledPos) {
		//	Take the current position
		//	Add
		//		The Vector between the two entities
		//		which has been normalized
		//  	then scaled to the distance between the stock position and the coupler
		//
		//	This works remarkably well even around corners
		return myPos.position.add(coupledPos.position.subtract(myPos.position).normalize().scale(getDefinition().getCouplerPosition(coupler, gauge)));
	}
	
	@Override
	protected void clearPositionCache() {
		super.clearPositionCache();
		couplerFrontPosition = null;
		couplerRearPosition = null;
	}

	public Vec3d getCouplerPosition(CouplerType coupler) {
		return getCouplerPosition(coupler, this.getCurrentTickPosOrFake());
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

	public Pair<EntityCoupleableRollingStock, CouplerType> potentialCouplings(CouplerType coupler) {
		List<EntityCoupleableRollingStock> train = this.getTrain();
		
		List<EntityCoupleableRollingStock> nearBy = getWorld().getEntities(entity -> {
			if (entity == null) {
				return false;
			}

			if (entity.isDead()) {
				return false;
			}

			if (entity.getPosition().distanceTo(this.getPosition()) > 64) {
				return false;
			}

			if (entity.gauge != EntityCoupleableRollingStock.this.gauge) {
				return false;
			}

			for (EntityCoupleableRollingStock stock : train) {
				if (stock.getUUID().equals(entity.getUUID())) {
					return false;
				}
			}

			return true;
		}, EntityCoupleableRollingStock.class);
		
		Pair<EntityCoupleableRollingStock, CouplerType> bestMatch = null;
		double bestDistance = 100;
		
		
		/*
		 * 1. |-----a-----| |-----b-----|
		 * 2. |-----a---|=|----b-----|
		 * 3. |---|=a====b|-----|
		 * Keep in mind that we want to make sure that our other coupler might be a better fit
		 */

		// getCouplerPosition is a somewhat expensive call, minimize if possible
		Vec3d myCouplerPos = this.getCouplerPosition(coupler);
		Vec3d myOppositeCouplerPos = this.getCouplerPosition(coupler.opposite());
		
		for (EntityCoupleableRollingStock stock : nearBy) {
			Vec3d stockFrontPos = stock.getCouplerPosition(CouplerType.FRONT);
			Vec3d stockBackPos = stock.getCouplerPosition(CouplerType.BACK);
			
			double couplerDistFront = getPosition().distanceTo(stockFrontPos);
			double couplerDistRear = getPosition().distanceTo(stockBackPos);
			
			// See above diagram (3).  OtherCoupler closet to my center is the one we want to couple to.
			CouplerType otherCoupler = couplerDistFront < couplerDistRear ? CouplerType.FRONT : CouplerType.BACK;
			if (stock.isCoupled(otherCoupler)) {
				//Best matching coupler is a no-go
				continue;
			}
			
			Vec3d stockCouplerPos = otherCoupler == CouplerType.FRONT ? stockFrontPos : stockBackPos;
			
			double myCouplerToOtherCoupler = myCouplerPos.distanceTo(stockCouplerPos);
			double myCenterToMyCoupler = getPosition().distanceTo(myCouplerPos);
			double myCenterToOtherCoupler = getPosition().distanceTo(stockCouplerPos);
			double myCouplerToOtherCenter = myCouplerPos.distanceTo(stock.getPosition());
			double myOppositeCouplerToOtherCenter = myOppositeCouplerPos.distanceTo(stock.getPosition());

			if (myCouplerToOtherCoupler > bestDistance) {
				// Current best match is closer, should be a small edge case when stock is almost entirely overlapping
				continue;
			}
			
			if (myCenterToMyCoupler < myCenterToOtherCoupler && this.isCouplerEngaged(coupler) && stock.isCouplerEngaged(otherCoupler)) {
				// diagram 1, check that it is not too far away
				if (myCouplerToOtherCoupler > ConfigDebug.couplerRange) {
					// Not close enough to consider
					continue;
				}
			} else {
				// diagram 2 or diagram 3
				RealBB myBB = this.getCollision().contract(new Vec3d(0, 0, 0.25)); // Prevent overlap on other rails
				if (!myBB.contains(stockCouplerPos)) {
					continue;
				}
			}
			
			if (myCouplerToOtherCenter > myOppositeCouplerToOtherCenter) {
				// My other coupler is a much better fit
				continue;
			}
			
			// findByUUID seems to work around a memcpy issue where refs are not updated
			stock = this.findByUUID(stock.getUUID());
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
		if (resimulateCooldown <= 0) {
			resimulate = true;
			resimulateCooldown = 5;
		}
	}
}
