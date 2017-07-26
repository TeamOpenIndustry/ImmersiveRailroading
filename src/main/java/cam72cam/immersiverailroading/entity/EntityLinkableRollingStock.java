package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityLinkableRollingStock extends EntityMoveableRollingStock {
	
	public enum CouplerType {
		FRONT(0),
		BACK(180);
		
		public final float yaw;

		CouplerType(float yaw) {
			this.yaw = yaw;
		}
	}
	
	public boolean isAttaching = false;

	private UUID LinkFront = null;
	private UUID LinkBack = null;
	private EntityLinkableRollingStock cartLinkedFront;
	private EntityLinkableRollingStock cartLinkedBack;

	public EntityLinkableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		if (LinkFront != null) {
			nbttagcompound.setString("LinkFront", LinkFront.toString());
		}
		if (LinkBack != null) {
			nbttagcompound.setString("LinkBack", LinkBack.toString());
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		if (nbttagcompound.hasKey("LinkFront")) {
			LinkFront = UUID.fromString(nbttagcompound.getString("LinkFront"));
			System.out.println("LINK FRONT");
		}
		
		if (nbttagcompound.hasKey("LinkBack")) {
			LinkBack = UUID.fromString(nbttagcompound.getString("LinkBack"));
			System.out.println("LINK BACK");
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		for (CouplerType coupler : CouplerType.values()) {
			if (!this.isLinked(coupler)) {
				for (EntityLinkableRollingStock potentialLink : this.potentialLinks(coupler)) {
					for (CouplerType potentialCoupler : CouplerType.values()) {
						// Is the coupler free?
						if (!potentialLink.isLinked(potentialCoupler)) {
							// Is the other coupler within linking distance?
							if (potentialLink.potentialLinks(potentialCoupler).contains(this)) {
								System.out.println(String.format("Linking %s to %s", this.getPersistentID(), potentialLink.getPersistentID()));
								this.setCoupledUUID(coupler, potentialLink.getPersistentID());
								this.setCoupledCart(coupler, potentialLink);
								potentialLink.setCoupledUUID(potentialCoupler, this.getPersistentID());
								potentialLink.setCoupledCart(potentialCoupler, this);
							}
						}
					}
					
					if (this.isLinked(coupler)) {
						// Got link
						break;
					}
					
					// False Match
					ImmersiveRailroading.logger.info(String.format("MISS %s %s %s", coupler, this.getPersistentID(), potentialLink.getPersistentID()));
				}
			}
		}
	}
	
	public Vec3d getCouplerPosition(CouplerType coupler) {
		return VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler), rotationYaw + coupler.yaw).add(getPositionVector());
	}
	
	public List<EntityLinkableRollingStock> potentialLinks(CouplerType coupler) {
		return getInCouplerRange(VecUtil.fromYaw(getDefinition().getCouplerPosition(coupler) + Config.couplerRange, rotationYaw + coupler.yaw).add(getPositionVector()));
	}
	
	private List<EntityLinkableRollingStock> getInCouplerRange(Vec3d pos) {
		AxisAlignedBB bb  = new AxisAlignedBB(-Config.couplerRange, -Config.couplerRange, -Config.couplerRange, Config.couplerRange, Config.couplerRange, Config.couplerRange);
		List<EntityLinkableRollingStock> inRange = world.getEntitiesWithinAABB(EntityLinkableRollingStock.class, bb.offset(pos).offset(0, 1, 0));
		inRange.remove(this); // just to be safe
		return inRange;
	}


	public boolean isLinked(EntityLinkableRollingStock cart) {
		return this.getLinkedCart(CouplerType.FRONT) == cart || this.getLinkedCart(CouplerType.BACK) == cart;
	}
	
	public final boolean isLinked() {
		return isLinked(CouplerType.FRONT) && isLinked(CouplerType.BACK);
	}
	
	public final boolean isLinked(CouplerType coupler) {
		return getCoupledUUID(coupler) != null;
	}
	
	public final UUID getCoupledUUID(CouplerType coupler) {
		switch(coupler) {
		case FRONT:
			return LinkFront;
		case BACK:
			return LinkBack;
		default:
			return null;
		}
	}
	public final void setCoupledUUID(CouplerType coupler, UUID id) {
		switch(coupler) {
		case FRONT:
			LinkFront = id;
			break;
		case BACK:
			LinkBack = id;
			break;
		}
	}
	
	public void unlink() {
		unlink(CouplerType.FRONT);
		unlink(CouplerType.BACK);
	}

	public void unlink(EntityLinkableRollingStock train) {
		if (train.getPersistentID() == this.getCoupledUUID(CouplerType.FRONT)) {
			unlink(CouplerType.FRONT);
		} else if (train.getPersistentID() == this.getCoupledUUID(CouplerType.BACK)) {
			unlink(CouplerType.BACK);
		}
	}

	public void unlink(CouplerType coupler) {
		EntityLinkableRollingStock cartLinked = getLinkedCart(coupler);

		// Break the link
		this.setCoupledUUID(coupler, null);
		this.setCoupledCart(coupler, null);

		// Ask the connected car to do the same
		if (cartLinked != null) {
			cartLinked.unlink(this);
		}
	}

	public void setCoupledCart(CouplerType coupler, EntityLinkableRollingStock cart) {
		switch (coupler) {
		case FRONT:
			this.cartLinkedFront = cart;
			break;
		case BACK:
			this.cartLinkedBack = cart;
			break;
		}
	}
	
	public EntityLinkableRollingStock getLinkedCart(CouplerType coupler) {
		EntityLinkableRollingStock cart = null;

		switch (coupler) {
		case FRONT:
			cart = this.cartLinkedFront;
			break;
		case BACK:
			cart = this.cartLinkedBack;
			break;
		}
		
		if (cart == null && this.getCoupledUUID(coupler) != null) {
			switch (coupler) {
			case FRONT:
				this.cartLinkedFront = findByUUID(this.world, this.getCoupledUUID(coupler));
				return this.cartLinkedFront;
			case BACK:
				this.cartLinkedBack = findByUUID(this.world, this.getCoupledUUID(coupler));
				return this.cartLinkedBack;
			}
		}
		return cart;
	}

	public static EntityLinkableRollingStock findByUUID(World world, UUID uuid) {
		// May want to cache this if it happens a lot
		for (Object e : world.getLoadedEntityList()) {
			if (e instanceof EntityLinkableRollingStock) {
				EntityLinkableRollingStock train = (EntityLinkableRollingStock) e;
				if (train.getPersistentID() == uuid) {
					return train;
				}
			}
		}
		return null;
	}
	
	public final List<EntityLinkableRollingStock> getTrain() {
		return this.buildTrain(new ArrayList<EntityLinkableRollingStock>());
	}
	private final List<EntityLinkableRollingStock> buildTrain(List<EntityLinkableRollingStock> train) {
		if (!train.contains(this)) {
			train.add(this);
			if (this.getLinkedCart(CouplerType.FRONT) != null) {
				train = this.getLinkedCart(CouplerType.FRONT).buildTrain(train);
			}
			if (this.getLinkedCart(CouplerType.BACK) != null) {
				train = this.getLinkedCart(CouplerType.BACK).buildTrain(train);
			}
		}
		return train;
	}
	

	
	public void moveLinkedRollingStock(Float moveDistance) {
		this.moveRollingStock(moveDistance);
		if (Math.abs(moveDistance) > 0.01) {
			recursiveMove(null);
		}
	}
	
	// This breaks with looped rolling stock
	// TODO prevent looped trains
	private void recursiveMove(EntityLinkableRollingStock prev) {
		for (CouplerType coupler : CouplerType.values()) {
			EntityLinkableRollingStock coupled = this.getLinkedCart(coupler);
			Vec3d myOffset = this.getCouplerPosition(coupler);
			
			if (coupled == null || coupled == prev) {
				// Either end of train or wrong iteration direction
				return;
			}
			
			Vec3d otherOffset = null;
			for(CouplerType otherCoupler : CouplerType.values()) {
				if (coupled.getLinkedCart(otherCoupler) == this) {
					// Matching coupler pair
					otherOffset = coupled.getCouplerPosition(otherCoupler);
				}
			}
			if (otherOffset == null) {
				ImmersiveRailroading.logger.warn("Broken Linkage %s => %s", this.getPersistentID(), coupled.getPersistentID());
				return;
			}
			
			double distance = myOffset.distanceTo(otherOffset);
			
			//TEMP
			if (distance > 1) {
				ImmersiveRailroading.logger.warn(String.format("%s too far from %s", this.getPersistentID(), coupled.getPersistentID()));
				ImmersiveRailroading.logger.warn(String.format("%s --> %s", myOffset, otherOffset));
				return;
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
}
