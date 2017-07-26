package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.ParticleUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class EntityLinkableRollingStock extends EntityMoveableRollingStock {
	
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
		
		if (!this.isLinkedFront()) {
			for (EntityLinkableRollingStock potentialLink : this.potentialFrontLinks()) {
				if (!potentialLink.isLinkedFront() && potentialLink.potentialFrontLinks().contains(this)) {
					ImmersiveRailroading.logger.info(String.format("COUPLING %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
					this.LinkFront = potentialLink.getPersistentID();
					potentialLink.LinkFront = this.getPersistentID();
					break;
				} 
				if (!potentialLink.isLinkedRear() && potentialLink.potentialRearLinks().contains(this)) {
					ImmersiveRailroading.logger.info(String.format("COUPLING %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
					this.LinkFront = potentialLink.getPersistentID();
					potentialLink.LinkBack = this.getPersistentID();
					break;
				}
				ImmersiveRailroading.logger.info(String.format("MISS FRONT %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
			}
		}
		if (!this.isLinkedRear()) {
			for (EntityLinkableRollingStock potentialLink : this.potentialRearLinks()) {
				if (!potentialLink.isLinkedFront() && potentialLink.potentialFrontLinks().contains(this)) {
					ImmersiveRailroading.logger.info(String.format("COUPLING %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
					this.LinkBack = potentialLink.getPersistentID();
					potentialLink.LinkFront = this.getPersistentID();
					break;
				} 
				if (!potentialLink.isLinkedRear() && potentialLink.potentialRearLinks().contains(this)) {
					ImmersiveRailroading.logger.info(String.format("COUPLING %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
					this.LinkBack = potentialLink.getPersistentID();
					potentialLink.LinkBack = this.getPersistentID();
					break;
				}
				ImmersiveRailroading.logger.info(String.format("MISS REAR %s %s", this.getPersistentID(), potentialLink.getPersistentID()));
			}
		}
	}
	
	public Vec3d frontCouplerPosition() {
		return VecUtil.fromYaw(getDefinition().getCouplerFront(), rotationYaw).add(getPositionVector());
	}
	
	public Vec3d rearCouplerPosition() {
		return VecUtil.fromYaw(getDefinition().getCouplerRear(), rotationYaw + 180).add(getPositionVector());
	}
	
	public List<EntityLinkableRollingStock> potentialFrontLinks() {
		ParticleUtil.spawnParticle(world, EnumParticleTypes.SMOKE_LARGE, VecUtil.fromYaw(getDefinition().getCouplerFront() + Config.couplerRange, rotationYaw).add(getPositionVector()));
		return getInCouplerRange(VecUtil.fromYaw(getDefinition().getCouplerFront() + Config.couplerRange, rotationYaw).add(getPositionVector()));
	}
	public List<EntityLinkableRollingStock> potentialRearLinks() {
		ParticleUtil.spawnParticle(world, EnumParticleTypes.REDSTONE, VecUtil.fromYaw(getDefinition().getCouplerRear() + Config.couplerRange, rotationYaw + 180).add(getPositionVector()));
		ParticleUtil.spawnParticle(world, EnumParticleTypes.REDSTONE, VecUtil.fromYaw(getDefinition().getCouplerRear() + Config.couplerRange, rotationYaw + 180).add(getPositionVector()));
		ParticleUtil.spawnParticle(world, EnumParticleTypes.REDSTONE, VecUtil.fromYaw(getDefinition().getCouplerRear() + Config.couplerRange, rotationYaw + 180).add(getPositionVector()));
		return getInCouplerRange(VecUtil.fromYaw(getDefinition().getCouplerRear() + Config.couplerRange, rotationYaw + 180).add(getPositionVector()));
	}
	private List<EntityLinkableRollingStock> getInCouplerRange(Vec3d pos) {
		AxisAlignedBB bb  = new AxisAlignedBB(-Config.couplerRange, -Config.couplerRange, -Config.couplerRange, Config.couplerRange, Config.couplerRange, Config.couplerRange);
		List<EntityLinkableRollingStock> inRange = world.getEntitiesWithinAABB(EntityLinkableRollingStock.class, bb.offset(pos).offset(0, 1, 0));
		inRange.remove(this); // just to be safe
		return inRange;
	}

	public final boolean isLinked() {
		return isLinkedFront() && isLinkedRear();
	}
	
	public final boolean isLinkedFront() {
		return LinkFront != null;
	}
	
	public final boolean isLinkedRear() {
		return LinkBack != null;
	}

	public void unlink() {
		unlinkFront();
		unlinkBack();
	}

	public void unlink(EntityLinkableRollingStock train) {
		if (train.getPersistentID() == this.LinkFront) {
			unlinkFront();
		} else if (train.getPersistentID() == this.LinkBack) {
			unlinkBack();
		}
	}

	public void unlinkFront() {
		EntityLinkableRollingStock cartLinkedFront = getLinkedCartFront();

		// Break the link
		this.LinkFront = null;
		this.cartLinkedFront = null;

		// Ask the connected car to do the same
		if (cartLinkedFront != null) {
			cartLinkedFront.unlink(this);
		}
	}

	public void unlinkBack() {
		EntityLinkableRollingStock cartLinkedBack = getLinkedCartBack();

		// Break the link
		this.LinkBack = null;
		this.cartLinkedBack = null;

		// Ask the connected car to do the same
		if (cartLinkedBack != null) {
			cartLinkedBack.unlink(this);
		}
	}

	public boolean isLinked(EntityLinkableRollingStock cart) {
		return this.getLinkedCartFront() == cart || this.getLinkedCartBack() == cart;
	}

	public EntityLinkableRollingStock getLinkedCartFront() {
		if (this.cartLinkedFront == null && this.LinkFront != null) {
			this.cartLinkedFront = findByUUID(this.world, this.LinkFront);
		}
		return this.cartLinkedFront;
	}

	public EntityLinkableRollingStock getLinkedCartBack() {
		if (this.cartLinkedBack == null && this.LinkBack != null) {
			this.cartLinkedBack = findByUUID(this.world, this.LinkBack);
		}
		return this.cartLinkedBack;
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
			if (this.getLinkedCartFront() != null) {
				train = this.getLinkedCartFront().buildTrain(train);
			}
			if (this.getLinkedCartBack() != null) {
				train = this.getLinkedCartBack().buildTrain(train);
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
		EntityLinkableRollingStock next = this.getLinkedCartBack();
		Vec3d myOffset = this.rearCouplerPosition();
		if (next == prev) {
			next = this.getLinkedCartFront();
			myOffset = this.frontCouplerPosition();
		}
		
		if (next == null) {
			// end of train
			// base case
			return;
		}
		
		Vec3d otherOffset = null;
		if (next.getLinkedCartBack() == this) {
			otherOffset = next.rearCouplerPosition();
		} else if (next.getLinkedCartFront() == this) {
			otherOffset = next.frontCouplerPosition();
		} else {
			ImmersiveRailroading.logger.warn("Broken Linkage %s => %s", this.getPersistentID(), next.getPersistentID());
			return;
		}
		
		double distance = myOffset.subtract(otherOffset).lengthVector();
		
		// Figure out which direction to move the next stock
		Vec3d nextPosForward = otherOffset.add(VecUtil.fromYaw(distance, next.rotationYaw));
		Vec3d nextPosReverse = otherOffset.add(VecUtil.fromYaw(-distance, next.rotationYaw));
		
		if (myOffset.distanceTo(nextPosForward) > myOffset.distanceTo(nextPosReverse)) {
			// Moving in reverse
			distance = -distance;
		}
		
		next.moveRollingStock(distance);
		next.recursiveMove(this);
	}
}
