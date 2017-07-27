package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityMoveableRollingStock extends EntityRidableRollingStock {

	public Float frontYaw;
	public Float rearYaw;

	public EntityMoveableRollingStock(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		frontYaw = additionalData.readFloat();
		rearYaw = additionalData.readFloat();
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeFloat(frontYaw);
		buffer.writeFloat(rearYaw);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		if (frontYaw != null) {
			nbttagcompound.setFloat("frontYaw", frontYaw);
		}
		if (rearYaw != null) {
			nbttagcompound.setFloat("rearYaw", rearYaw);
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		if (nbttagcompound.hasKey("frontYaw")) {
			frontYaw = nbttagcompound.getFloat("frontYaw");
		}
		if (nbttagcompound.hasKey("rearYaw")) {
			rearYaw = nbttagcompound.getFloat("rearYaw");
		}
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return this.getDefinition().getBounds(this);
	}
	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		return this.getDefinition().getBounds(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport){
		// We need our own custom sync packets, see MRSSyncPacket
    }
	@Override
	public void setVelocity(double x, double y, double z) {
		// We need our own custom sync packets, see MRSSyncPacket
	}
	
	public void moveRollingStock(double moveDistance) {
		if (moveDistance == 0) {
			return;
		}
		if (frontYaw == null) {
			frontYaw = rotationYaw;
		}
		if (rearYaw == null) {
			rearYaw = rotationYaw;
		}
		
		this.prevRotationYaw = rotationYaw;
		
		boolean isReverse = moveDistance < 0;
		
		if (isReverse) {
			moveDistance = -moveDistance;
			frontYaw += 180;
			rearYaw += 180;
			rotationYaw += 180;
			this.rotationYaw = (this.rotationYaw + 360f) % 360f;
			frontYaw = (frontYaw + 360f) % 360f;
			rearYaw = (rearYaw + 360f) % 360f;
		}

		Vec3d front = frontBogeyPosition();
		Vec3d rear = rearBogeyPosition();
		Vec3d nextFront = nextPosition(front, this.frontYaw, nextMovement(this.frontYaw, moveDistance));
		Vec3d nextRear = nextPosition(rear, this.rearYaw, nextMovement(this.rearYaw, moveDistance));
		Vec3d frontDelta = front.subtractReverse(nextFront);
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		frontYaw = VecUtil.toYaw(frontDelta);
		rearYaw = VecUtil.toYaw(rearDelta);

		Vec3d currCenter = between(front, rear);
		Vec3d nextCenter = between(nextFront, nextRear);
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = nextRear.subtractReverse(nextFront);
		rotationYaw = VecUtil.toYaw(bogeySkew);
		
		
		if (isReverse) {
			frontYaw += 180;
			rearYaw += 180;
			rotationYaw += 180;
			rotationYaw = (rotationYaw + 360f) % 360f;
			frontYaw = (frontYaw + 360f) % 360f;
			rearYaw = (rearYaw + 360f) % 360f;
		}

		this.motionX = deltaCenter.x;
		this.motionZ = deltaCenter.z;

		// Can this run client side in 1.12?
		if (!this.world.isRemote && world.isAirBlock(new BlockPos((int) posX, (int) (this.posY - 0.6), (int) posZ))) {
			// Fall
			if (this.motionY < 1) {
				this.motionY += -0.1;
			}
		} else {
			this.motionY = 0;
		}

		this.prevPosX = posX;
		this.prevPosY = posY;
		this.prevPosZ = posZ;
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		
		if (!this.world.isRemote && this.ticksExisted % 20 == 0) {
			this.sendToObserving(new MRSSyncPacket(this));
		}
		
		List<Entity> entitiesWithin = world.getEntitiesWithinAABB(Entity.class, this.getCollisionBoundingBox());
		for (Entity entity : entitiesWithin) {
			if (entity instanceof EntityMoveableRollingStock) {
				// rolling stock collisions handled by looking at the front and rear coupler offsets
				continue;
			}
			
			if (entity.getRidingEntity() instanceof EntityMoveableRollingStock) {
				// Don't apply bb to passengers
				continue;
			}
			
			// Move entity
			entity.setVelocity(this.motionX * 2, 0, this.motionZ * 2);
			// Force update
			entity.onUpdate();
			
			double speedDamage = this.getCurrentSpeed().metric() / Config.entitySpeedDamage;
			if (speedDamage > 1) {
				entity.attackEntityFrom((new DamageSource("hitByTrain")).setDamageBypassesArmor(), (float) speedDamage);
			}
		}
		
		// Riding on top of carts
		AxisAlignedBB bb = this.getCollisionBoundingBox();
		bb = bb.offset(0, bb.maxY - bb.minY, 0);
		bb = bb.setMaxY(bb.minY + 1);
		List<Entity> entitiesAbove = world.getEntitiesWithinAABB(Entity.class, bb);
		for (Entity entity : entitiesAbove) {
			if (entity instanceof EntityMoveableRollingStock) {
				continue;
			}
			Vec3d pos = entity.getPositionVector();
			pos = pos.addVector(this.motionX, this.motionY, this.motionZ);
			entity.setPosition(pos.x, pos.y, pos.z);
		}
	}

	private Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}
	
	public Vec3d frontBogeyPosition() {
		return VecUtil.fromYaw(this.getDefinition().getBogeyFront(), rotationYaw).add(getPositionVector());
	}

	public Vec3d rearBogeyPosition() {
		return VecUtil.fromYaw(this.getDefinition().getBogeyRear(), rotationYaw).add(getPositionVector());
	}

	private TileRail railFromPosition(Vec3d position) {
		TileEntity te = world.getTileEntity(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
		if (te instanceof TileRailGag) {
			return (TileRail) world.getTileEntity(((TileRailGag) te).getParent());
		} else if (te instanceof TileRail) {
			return (TileRail) te;
		} else {
			return null;
		}
	}

	private Vec3d nextMovement(float yaw, double d) {
		return VecUtil.fromYaw(d, yaw);
	}

	private Vec3d nextPosition(Vec3d position, float yaw, Vec3d delta) {
		TileRail rail = railFromPosition(position);

		if (rail == null) {
			if (!world.isRemote ) {
				System.out.println("WARNING OFF TRACK!!!");
				System.out.println(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
				System.out.println(world.getBlockState(new BlockPos(position)).getBlock().getLocalizedName());
				this.setDead();
				return position;
			} else {
				return position.add(delta);
			}
		}

		double distance = delta.lengthVector();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = new Vec3d(rail.getCenter()).subtractReverse(position);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = Math.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() + 1; // TODO bake this into
													// BuilderTurn
			// Calculate the angle delta in rad (radians are awesome)
			double yawDelt = distance / radius;

			// Calculate the original next position (won't be accurate, but we
			// use it as an estimate)
			Vec3d nextPos = position.add(delta);

			/*
			 * for (int i = 0; i < 90; i++) { Vec3d check = new
			 * Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw +
			 * Math.toRadians(i)) * radius, 0, -Math.cos(posRelYaw +
			 * Math.toRadians(i)) * radius);
			 * world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, check.x,
			 * check.y, check.z, 0, 0, 0); }
			 */

			// Calculate the two possible next positions (forward on the curve
			// or backward on the curve)
			Vec3d newpos = new Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw + yawDelt) * radius, 0, -Math.cos(posRelYaw + yawDelt) * radius);
			Vec3d newneg = new Vec3d(rail.getCenter()).addVector(Math.sin(posRelYaw - yawDelt) * radius, 0, -Math.cos(posRelYaw - yawDelt) * radius);

			// Return whichever position is closest to the estimated next
			// position
			if (newpos.subtract(nextPos).lengthVector() < newneg.subtract(nextPos).lengthVector()) {
				return newpos;
			} else {
				return newneg;
			}
		} else {
			// Good enough for now
			return position.add(delta);
			// Look on either side of the rail for a sibling rail
			/*
			 * Vec3d side1Pos = position.add(delta.rotateYaw(90)); Vec3d
			 * side2Pos = position.add(delta.rotateYaw(-90)); TileRail side1Rail
			 * = railFromPosition(side1Pos); TileRail side2Rail =
			 * railFromPosition(side2Pos); Vec3d betweenLoc; if (side1Rail !=
			 * null && side1Rail.getParent() == rail.getParent()) { betweenLoc =
			 * between(new Vec3d(side1Rail.getPos()), new Vec3d(rail.getPos()));
			 * } else if (side2Rail != null && side2Rail.getParent() ==
			 * rail.getParent()) { betweenLoc = between(new
			 * Vec3d(side2Rail.getPos()), new Vec3d(rail.getPos())); } else {
			 * ImmersiveRailroading.logger.error("INVALID RAIL"); return
			 * position.add(delta); } if (Math.abs(delta.x) > Math.abs(delta.z))
			 * { return new Vec3d(position.x + Math.copySign(distance, delta.x),
			 * position.y, Math.floor(position.z) + betweenLoc.z); } else {
			 * return new Vec3d(betweenLoc.x, position.y, position.z +
			 * Math.copySign(distance, delta.z)); }
			 */
		}
	}
}
