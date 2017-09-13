package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
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
	public int tickPosID = 0;
	private Speed currentSpeed;
	public List<TickPos> positions = new ArrayList<TickPos>();

	public EntityMoveableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		frontYaw = BufferUtil.readFloat(additionalData);
		rearYaw = BufferUtil.readFloat(additionalData);
		tickPosID = additionalData.readInt();
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		BufferUtil.writeFloat(buffer, frontYaw);
		BufferUtil.writeFloat(buffer, rearYaw);
		buffer.writeInt(tickPosID);

		this.sendToObserving(new MRSSyncPacket(this, this.positions));
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
		
		if (frontYaw == null) {
			frontYaw = rotationYaw;
		}
		if (rearYaw == null) {
			rearYaw = rotationYaw;
		}
		initPositions();
	}
	
	public void initPositions() {
		this.positions = new ArrayList<TickPos>();
		this.positions.add(new TickPos(this.tickPosID, this.getCurrentSpeed(), this.getPositionVector(), this.rotationYaw, this.rotationYaw, this.rotationYaw, this.rotationPitch, false, this.isReverse));
	}

	/*
	 * Entity Overrides for BB
	 */

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
    public AxisAlignedBB getRenderBoundingBox()
    {
        return this.getEntityBoundingBox().grow(20);
    }
	
	/*
	 * Disable standard entity sync
	 */

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
		// We need our own custom sync packets, see MRSSyncPacket
	}

	@Override
	public void setVelocity(double x, double y, double z) {
		// We need our own custom sync packets, see MRSSyncPacket
	}
	
	/*
	 * Speed Info
	 */

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
	public void setCurrentSpeed(Speed newSpeed) {
		this.currentSpeed = newSpeed;
	}
	

	public void handleTickPosPacket(List<TickPos> newPositions) {
		// TODO: Speed up or slow down entity ticking instead of
		// skipping or re-doing ticks
		this.positions = newPositions;
		this.tickPosID = newPositions.get(0).tickID;
	}
	
	public TickPos getTickPos(int tickID) {
		for (TickPos pos : positions) {
			if (pos.tickID == tickID) {
				return pos;
			}
		}

		ImmersiveRailroading.logger.warn("Missing TickPos for " + tickID);
		
		return positions.get(positions.size()-1);
	}
	
	public TickPos getCurrentTickPos() {
		if (positions.size() == 0) {
			return null;
		}
		if (positions.get(0).tickID != this.tickPosID) {
			// Prune list
			while (positions.get(0).tickID != this.tickPosID && positions.size() > 1) {
				positions.remove(0);
			}
		}
		return positions.get(0);
	}
	
	public int getRemainingPositions() {
		return positions.size();
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		this.tickPosID++;
		
		// Apply position tick
		TickPos currentPos = getCurrentTickPos();
		if (currentPos == null) {
			// Not loaded yet or not moving
			return;
		}
		if (currentPos.isOffTrack) {
			world.removeEntity(this);
			return;
		}
		
	    this.prevPosX = this.posX;
	    this.prevPosY = this.posY;
	    this.prevPosZ = this.posZ;
	    this.lastTickPosX = this.posX;
	    this.lastTickPosY = this.posY;
	    this.lastTickPosZ = this.posZ;
	    this.prevRotationYaw = this.rotationYaw;
	    this.prevRotationPitch = this.rotationPitch;
	    
	    this.posX = currentPos.position.x;
	    this.posY = currentPos.position.y;
	    this.posZ = currentPos.position.z;
	    this.rotationYaw = currentPos.rotationYaw;
	    this.rotationPitch = currentPos.rotationPitch;
	    this.frontYaw = currentPos.frontYaw;
	    this.rearYaw = currentPos.rearYaw;
	    
	    this.currentSpeed = currentPos.speed;
	    this.isReverse = currentPos.isReverse; 
	    this.motionX = this.posX - this.prevPosX;
	    this.motionY = this.posY - this.prevPosY;
	    this.motionZ = this.posZ - this.prevPosZ;
	    
	    distanceTraveled += this.currentSpeed.minecraft();

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

	public TickPos moveRollingStock(double moveDistance, int lastTickID) {
		TickPos lastPos = this.getTickPos(lastTickID);
		return new MovementSimulator(world, lastPos, this.getDefinition().getBogeyFront(), this.getDefinition().getBogeyRear()).nextPosition(moveDistance);
	}
	
	/*
	 * 
	 * Client side render guessing
	 */
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
		MovementSimulator sim = new MovementSimulator(world, getCurrentTickPos(), this.getDefinition().getBogeyFront(), this.getDefinition().getBogeyRear());
		
		Vec3d front = sim.frontBogeyPosition();
		Vec3d nextFront = front;
		while (offset > 0) {
			nextFront = sim.nextPosition(nextFront, this.rotationYaw, VecUtil.fromYaw(Math.min(0.1, offset), this.frontYaw));
			offset -= 0.1;
		}
		Vec3d frontDelta = front.subtractReverse(nextFront);
		return new PosRot(nextFront.subtractReverse(this.getPositionVector()), VecUtil.toYaw(frontDelta));
	}
	public PosRot predictRearBogeyPosition(float offset) {
		MovementSimulator sim = new MovementSimulator(world, getCurrentTickPos(), this.getDefinition().getBogeyFront(), this.getDefinition().getBogeyRear());
		
		Vec3d rear = sim.rearBogeyPosition();
		Vec3d nextRear = rear;
		while (offset > 0) {
			nextRear = sim.nextPosition(nextRear, this.rotationYaw+180, VecUtil.fromYaw(Math.min(0.1, offset), this.rearYaw+180));
			offset -= 0.1;
		}
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		return new PosRot(nextRear.subtractReverse(this.getPositionVector()), VecUtil.toYaw(rearDelta));
	}
}
