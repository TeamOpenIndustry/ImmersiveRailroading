package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.SwitchUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityMoveableRollingStock extends EntityRidableRollingStock {

	public Float frontYaw;
	public Float rearYaw;
	public boolean isReverse = false;
	public float distanceTraveled = 0;

	public EntityMoveableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		frontYaw = BufferUtil.readFloat(additionalData);
		rearYaw = BufferUtil.readFloat(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		BufferUtil.writeFloat(buffer, frontYaw);
		BufferUtil.writeFloat(buffer, rearYaw);
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
		nbttagcompound.setFloat("distanceTraveled", distanceTraveled);
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
		distanceTraveled = nbttagcompound.getFloat("distanceTraveled");
	}
	
	public void zeroSpeed() {
		motionX = 0;
		motionY = 0;
		motionZ = 0;
		currentSpeed = null;
	}

	private Speed currentSpeed;
	public Speed getCurrentSpeed() {
		if (currentSpeed == null) {
			//Fallback
			// does not work for curves
			float speed = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
			if (Float.isNaN(speed)) {
				speed = 0;
			}
			currentSpeed = Speed.fromMinecraft(speed);
		}
		return currentSpeed;
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
	public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
		// We need our own custom sync packets, see MRSSyncPacket
	}

	@Override
	public void setVelocity(double x, double y, double z) {
		// We need our own custom sync packets, see MRSSyncPacket
	}
	
	public void syncMRS() {
		this.sendToObserving(new MRSSyncPacket(this));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		// Need to do this here instead of in moveRollingStock
		// calling this too often can get packets mixed up (eg recursive case)
		if (!this.world.isRemote && this.ticksExisted % 20 == 0) {
			this.syncMRS();
		}

		List<Entity> entitiesWithin = world.getEntitiesWithinAABB(Entity.class, this.getCollisionBoundingBox());
		for (Entity entity : entitiesWithin) {
			if (entity instanceof EntityMoveableRollingStock) {
				// rolling stock collisions handled by looking at the front and
				// rear coupler offsets
				continue;
			}

			if (entity.getRidingEntity() instanceof EntityMoveableRollingStock) {
				// Don't apply bb to passengers
				continue;
			}

			// Move entity
			
			entity.motionX = this.motionX * 2;
			entity.motionY = 0;
			entity.motionZ = this.motionZ * 2;
			// Force update
			entity.onUpdate();

			double speedDamage = this.getCurrentSpeed().metric() / Config.entitySpeedDamage;
			if (speedDamage > 1) {
				entity.attackEntityFrom((new DamageSource("hitByTrain")).setDamageBypassesArmor(), (float) speedDamage);
			}
		}

		// Riding on top of cars
		AxisAlignedBB bb = this.getCollisionBoundingBox();
		bb = bb.offset(0, this.getDefinition().getHeight()+1, 0);
		List<Entity> entitiesAbove = world.getEntitiesWithinAABB(Entity.class, bb);
		for (Entity entity : entitiesAbove) {
			if (entity instanceof EntityMoveableRollingStock) {
				continue;
			}
			if (this.isPassenger(entity)) {
				continue;
			}
			Vec3d pos = entity.getPositionVector();
			pos = pos.addVector(this.motionX, this.motionY, this.motionZ);
			entity.setPosition(pos.x, pos.y, pos.z);
		}
	}
	
	public class PosRot extends Vec3d {
		private float rotation;
		public PosRot(double xIn, double yIn, double zIn, float rotation) {
			super(xIn, yIn, zIn);
			this.rotation = rotation;
		}
		public PosRot(Vec3d nextFront, float yaw) {
			this(nextFront.x, nextFront.y, nextFront.z, yaw);
		}
		public float getRotation() {
			return rotation;
		}
	}

	public PosRot predictFrontBogeyPosition(float offset) {
		Vec3d front = frontBogeyPosition();
		Vec3d nextFront = front;
		while (offset > 0) {
			nextFront = nextPosition(nextFront, this.rotationYaw, nextMovement(this.frontYaw, Math.min(0.1, offset)));
			offset -= 0.1;
		}
		Vec3d frontDelta = front.subtractReverse(nextFront);
		return new PosRot(nextFront.subtractReverse(this.getPositionVector()), VecUtil.toYaw(frontDelta));
	}
	public PosRot predictRearBogeyPosition(float offset) {
		Vec3d rear = rearBogeyPosition();
		Vec3d nextRear = rear;
		while (offset > 0) {
			nextRear = nextPosition(nextRear, this.rotationYaw+180, nextMovement(this.rearYaw+180, Math.min(0.1, offset)));
			offset -= 0.1;
		}
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		return new PosRot(nextRear.subtractReverse(this.getPositionVector()), VecUtil.toYaw(rearDelta));
	}

	public void moveRollingStock(double moveDistance) {
		if (Math.abs(moveDistance) > 0.1) {
			//Split movement (recursive)
			
			// Save current data to apply at the end
		    double prevPosX = this.posX;
		    double prevPosY = this.posY;
		    double prevPosZ = this.posZ;
		    float prevRotationYaw = this.rotationYaw;
		    float prevRotationPitch = this.rotationPitch;
		    double lastTickPosX = this.posX;
		    double lastTickPosY = this.posY;
		    double lastTickPosZ = this.posZ;
		    
		    double motionX = 0;
		    double motionY = 0;
		    double motionZ = 0;
		    
		    moveRollingStock(moveDistance/2);
		    motionX += this.motionX;
		    motionY += this.motionY;
		    motionZ += this.motionZ;
		    moveRollingStock(moveDistance/2);
		    motionX += this.motionX;
		    motionY += this.motionY;
		    motionZ += this.motionZ;
		    
		    this.motionX = motionX;
		    this.motionY = motionY;
		    this.motionZ = motionZ;
		    
		    // Apply prev position info now that we have performed the movement
		    this.prevPosX = prevPosX;
		    this.prevPosY = prevPosY;
		    this.prevPosZ = prevPosZ;
		    this.prevRotationYaw = prevRotationYaw;
		    this.prevRotationPitch = prevRotationPitch;
		    this.lastTickPosX = lastTickPosX;
		    this.lastTickPosY = lastTickPosY;
		    this.lastTickPosZ = lastTickPosZ;
		    
		    this.currentSpeed = Speed.fromMinecraft(Math.abs(moveDistance));
		    
		    return;
		}
	    
	    this.currentSpeed = Speed.fromMinecraft(Math.abs(moveDistance));
	    distanceTraveled += moveDistance;
		
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

		isReverse  = moveDistance < 0;

		Vec3d front = frontBogeyPosition();
		Vec3d rear = rearBogeyPosition();
		
		if (isReverse) {
			moveDistance = -moveDistance;
			frontYaw += 180;
			rearYaw += 180;
			rotationYaw += 180;
			rotationPitch = -rotationPitch;
			rotationYaw = (rotationYaw + 360f) % 360f;
			frontYaw = (frontYaw + 360f) % 360f;
			rearYaw = (rearYaw + 360f) % 360f;
		}

		
		
		Vec3d nextFront = nextPosition(front, this.rotationYaw, nextMovement(this.frontYaw, moveDistance));
		Vec3d nextRear = nextPosition(rear, this.rotationYaw, nextMovement(this.rearYaw, moveDistance));
		Vec3d frontDelta = front.subtractReverse(nextFront);
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		frontYaw = VecUtil.toYaw(frontDelta);
		rearYaw = VecUtil.toYaw(rearDelta);

		Vec3d currCenter = between(front, rear);
		Vec3d nextCenter = between(nextFront, nextRear);
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = nextRear.subtractReverse(nextFront);
		rotationYaw = VecUtil.toYaw(bogeySkew);
		rotationPitch = (float) Math.toDegrees(Math.atan2(bogeySkew.y, nextRear.distanceTo(nextFront)));

		if (isReverse) {
			frontYaw += 180;
			rearYaw += 180;
			//rotationYaw += 180;
			rotationPitch = -rotationPitch;
			rotationYaw = (rotationYaw + 360f) % 360f;
			frontYaw = (frontYaw + 360f) % 360f;
			rearYaw = (rearYaw + 360f) % 360f;
		}

		this.motionX = deltaCenter.x;
		this.motionY = deltaCenter.y;
		this.motionZ = deltaCenter.z;

		// Can this run client side in 1.12?
		if (!this.world.isRemote && world.isAirBlock(new BlockPos(this))) {
			// Fall
			if (this.motionY < 1) {
				this.motionY += -0.1;
			}
		}

		this.prevPosX = posX;
		this.prevPosY = posY;
		this.prevPosZ = posZ;
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
	}

	private Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}

	public Vec3d frontBogeyPosition() {
		return VecUtil.fromYawPitch(this.getDefinition().getBogeyFront(), rotationYaw, rotationPitch).add(getPositionVector());
	}

	public Vec3d rearBogeyPosition() {
		return VecUtil.fromYawPitch(this.getDefinition().getBogeyRear(), rotationYaw, rotationPitch).add(getPositionVector());
	}

	private TileRailBase directRailFromPosition(Vec3d position) {
		TileEntity te = world.getTileEntity(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
		if (te instanceof TileRailBase) {
			return (TileRailBase)te;
		}
		return null;
	}
	
	protected TileRail railFromPosition(Vec3d position) {
		TileEntity te = world.getTileEntity(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
		TileRail parent;
		if (te instanceof TileRailGag) {
			parent = ((TileRailGag) te).getParentTile();
		} else if (te instanceof TileRail) {
			parent = (TileRail) te;
		} else {
			return null;
		}
		
		TileRail super_parent = parent.getParentTile();
		if (SwitchUtil.getSwitchState(parent) == SwitchState.STRAIGHT) {
			parent = super_parent;
		}
		
		return parent;
	}

	private Vec3d nextMovement(float yaw, double d) {
		return VecUtil.fromYaw(d, yaw);
	}

	private Vec3d nextPosition(Vec3d position, float trainYaw, Vec3d delta) {
		TileRail rail = railFromPosition(position);
		if (rail == null) {
			// Try a smidge higher
			// We get some wobble on the top of slopes, this corrects for imperfect precision
			rail = railFromPosition(position.addVector(0, 0.2, 0));
			if (rail != null) {
				position = position.addVector(0, 0.2, 0);
			} else {
				rail = railFromPosition(position.addVector(0, -0.2, 0));
				if (rail != null) {
					position = position.addVector(0, -0.2, 0);
				}
			}
		}

		if (rail == null) {
			if (!world.isRemote) {
				System.out.println("WARNING OFF TRACK!!!");
				System.out.println(position);
				System.out.println(world.getBlockState(new BlockPos(position)).getBlock().getLocalizedName());
				this.setDead();
				return position;
			} else {
				return position.add(delta);
			}
		}

		// Update y position
		TileRailBase directRail = directRailFromPosition(position);
		position = new Vec3d(position.x, Math.floor(position.y) + directRail.getHeight(), position.z);

		double distance = delta.lengthVector();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = rail.getCenter().subtractReverse(position);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = Math.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() - 0.5;
			// Calculate the angle delta in rad (radians are awesome)
			double yawDelt = distance / radius;

			// Calculate the original next position (won't be accurate, but we
			// use it as an estimate)
			Vec3d nextPos = position.add(delta);

			
			 for (int i = 0; i < 90; i++) { Vec3d check = 
			 rail.getCenter().addVector(Math.sin(posRelYaw +
			 Math.toRadians(i)) * radius, 0, -Math.cos(posRelYaw +
			 Math.toRadians(i)) * radius);
			 world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, check.x,
			 check.y, check.z, 0, 0, 0); }
			 

			// Calculate the two possible next positions (forward on the curve
			// or backward on the curve)
			Vec3d newpos = rail.getCenter().addVector(Math.sin(posRelYaw + yawDelt) * radius, 0, -Math.cos(posRelYaw + yawDelt) * radius);
			Vec3d newneg = rail.getCenter().addVector(Math.sin(posRelYaw - yawDelt) * radius, 0, -Math.cos(posRelYaw - yawDelt) * radius);

			// Return whichever position is closest to the estimated next
			// position
			if (newpos.subtract(nextPos).lengthVector() < newneg.subtract(nextPos).lengthVector()) {
				return newpos;
			} else {
				return newneg;
			}
		} else {
			// delta should be in the direction of rotationYaw instead of front or rear
			// since large changes can occur if the train is way off center
			delta = nextMovement(trainYaw, distance);
			/*
			// Check that we are not moving in the wrong axis along a track
			if (EnumFacing.fromAngle(trainYaw).getAxis() != rail.getFacing().getAxis() && rail.getType() != TrackItems.CROSSING) {
				if (!world.isRemote) {
					System.out.println("Wrong track direction");
					this.setDead();
					return position;
				}
			}*/
			float angle = rail.getRotationQuarter()/4f * 90 + rail.getFacing().getHorizontalAngle();
			
			// |>----O-----|
			// |--->-O-----|
			// |-----O->---|
			// |-----O---->|
			// |<----O-----|
			// |---<-O-----|
			// |-----O-<---|
			// |-----O----<|
			
			
			Vec3d center = rail.getPlacementPosition();
			
			double toCenter = center.distanceTo(position);
			
			Vec3d possiblePositive = center.add(VecUtil.fromYaw(toCenter, angle));
			Vec3d possibleNegative = center.add(VecUtil.fromYaw(-toCenter, angle));
			
			double angularDistance = 0; 
			if (possiblePositive.distanceTo(position) < possibleNegative.distanceTo(position)) {
				angularDistance = toCenter;
			} else {
				angularDistance = -toCenter;
			}
			
			possiblePositive = center.add(VecUtil.fromYaw(angularDistance + distance, angle));
			possibleNegative = center.add(VecUtil.fromYaw(angularDistance - distance, angle));
			
			if (possiblePositive.distanceTo(position.add(delta)) < possibleNegative.distanceTo(position.add(delta))) {
				return possiblePositive;
			} else {
				return possibleNegative;
			}
		}
	}

	/**
	 * Should only be called from MRSSync
	 */
	public void setCurrentSpeed(Speed newSpeed) {
		this.currentSpeed = newSpeed;
	}
}
