package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityMoveableRollingStock extends EntityRidableRollingStock {

	private Float frontYaw;
	private Float rearYaw;
	public boolean isReverse = false;
	public float distanceTraveled = 0;
	public double tickPosID = 0;
	private double clientTicksPerServerTick = 1;
	private Speed currentSpeed;
	public List<TickPos> positions = new ArrayList<TickPos>();
	private AxisAlignedBB boundingBox;

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
		buffer.writeInt((int)tickPosID);

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
		this.positions.add(new TickPos((int)this.tickPosID, this.getCurrentSpeed(), this.getPositionVector(), this.rotationYaw, this.rotationYaw, this.rotationYaw, this.rotationPitch, false, this.isReverse));
	}

	public void initPositions(TickPos tp) {
		this.positions = new ArrayList<TickPos>();
		this.positions.add(tp);
	}

	/*
	 * Entity Overrides for BB
	 */

	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return this.getEntityBoundingBox();
	}

	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		if (this.boundingBox == null) {
			this.boundingBox = this.getDefinition().getBounds(this);
		}
		return this.boundingBox;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return this.getEntityBoundingBox().grow(50);
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
		if (this.positions != null && newPositions.size() != 0 && this.ticksExisted > 3) {
			double tickOffset = tickPosID - newPositions.get(0).tickID;
			
			if (tickOffset > 0) {
				clientTicksPerServerTick *= 1 - (Math.min(10, tickOffset) / 20); // Slow down client ticks
			}
			if (tickOffset < 0) {
				clientTicksPerServerTick *= 1 + (Math.min(10, -tickOffset) / 20); // Speed up client ticks
			}
		}
		this.positions = newPositions;
		if (newPositions.size() > 0) {
			// might happen if stock stops suddenly
			this.tickPosID = newPositions.get(0).tickID;
		}
	}
	
	public TickPos getTickPos(int tickID) {
		if (positions.size() == 0) {
			return null;
		}
		for (TickPos pos : positions) {
			if (pos.tickID == tickID) {
				return pos;
			}
		}
		
		return positions.get(positions.size()-1);
	}
	
	public TickPos getCurrentTickPosAndPrune() {
		if (positions.size() == 0) {
			return null;
		}
		if (positions.get(0).tickID != (int)this.tickPosID) {
			// Prune list
			while (positions.get(0).tickID < (int)this.tickPosID && positions.size() > 1) {
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
		
		this.tickPosID += clientTicksPerServerTick;
		
		// Apply position tick
		TickPos currentPos = getCurrentTickPosAndPrune();
		if (currentPos == null) {
			// Not loaded yet or not moving
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
	    
	    if (!currentPos.speed.isZero() || this.ticksExisted < 5) {
	    	this.boundingBox = null; // Force update
	    }
	    
	    this.currentSpeed = currentPos.speed;
	    this.isReverse = currentPos.isReverse; 
	    this.motionX = this.posX - this.prevPosX;
	    this.motionY = this.posY - this.prevPosY;
	    this.motionZ = this.posZ - this.prevPosZ;
	    
	    distanceTraveled += (this.isReverse ? -1 : 1) * this.currentSpeed.minecraft();

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

			if (! (entity instanceof EntityLivingBase)) {
				continue;
			}

			
			// Chunk.getEntitiesOfTypeWithinAABB() does a reverse aabb intersect
			// We need to do a forward lookup
			if (!this.getCollisionBoundingBox().intersects(entity.getEntityBoundingBox())) {
				// miss
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
			
			if (! (entity instanceof EntityLivingBase)) {
				continue;
			}

			// Chunk.getEntitiesOfTypeWithinAABB() does a reverse aabb intersect
			// We need to do a forward lookup
			if (!bb.intersects(entity.getEntityBoundingBox())) {
				// miss
				continue;
			}
			
			Vec3d pos = entity.getPositionVector();
			pos = pos.addVector(this.motionX, this.motionY, this.motionZ);
			entity.setPosition(pos.x, pos.y, pos.z);
		}
		if (this.ticksExisted % 20 == 0 && !world.isRemote) {
			bb = this.getCollisionBoundingBox();
			for (int x = MathHelper.floor(bb.minX); x <= MathHelper.ceil(bb.maxX); x++) {
				for (int y = MathHelper.floor(bb.minY)-1; y <= MathHelper.ceil(bb.maxY); y++) {
					for (int z = MathHelper.floor(bb.minZ); z <= MathHelper.ceil(bb.maxZ); z++) {
						BlockPos bp = new BlockPos(x, y, z);
						if (bb.contains(new Vec3d(bp).addVector(0.5, 0.5, 0.5))) {
							IBlockState state = world.getBlockState(bp);
							if (state.getBlock() != Blocks.AIR) {
								if (!BlockUtil.isRail(state)) {
									IBlockState up = world.getBlockState(bp.up());
									if (!BlockUtil.isRail(up)) {
										world.destroyBlock(bp, true);										
									}
								} else {
									TileRailBase te = TileRailBase.get(world, bp);
									if (te != null) {
										te.cleanSnow();
										continue;
									}
								}
							}
						}
					}
				}
			}
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

	
	public float getFrontYaw() {
		if (this.frontYaw != null) {
			return this.frontYaw;
		}
		return this.rotationYaw;
	}
	
	public float getRearYaw() {
		if (this.rearYaw != null) {
			return this.rearYaw;
		}
		return this.rotationYaw;
	}

	protected TickPos getCurrentTickPosOrFake() {
		if (this.getTickPos((int)this.tickPosID) != null) {
			return this.getTickPos((int)this.tickPosID);
		}
		return new TickPos(0, Speed.fromMetric(0), this.getPositionVector(), this.getFrontYaw(), this.getRearYaw(), this.rotationYaw, this.rotationPitch, false, false);
	}
	
	public PosRot predictFrontBogeyPosition(float offset) {		
		return predictFrontBogeyPosition(getCurrentTickPosOrFake(), offset);
	}
	public PosRot predictFrontBogeyPosition(TickPos pos, float offset) {		
		MovementSimulator sim = new MovementSimulator(world, pos, this.getDefinition().getBogeyFront(), this.getDefinition().getBogeyRear());
		
		Vec3d front = sim.frontBogeyPosition();
		Vec3d nextFront = front;
		while (offset > 0) {
			nextFront = sim.nextPosition(nextFront, pos.rotationYaw, VecUtil.fromYaw(Math.min(0.1, offset), pos.frontYaw));
			offset -= 0.1;
		}
		Vec3d frontDelta = front.subtractReverse(nextFront);
		return new PosRot(nextFront.subtractReverse(pos.position), VecUtil.toYaw(frontDelta));
	}
	
	public PosRot predictRearBogeyPosition(float offset) {		
		return predictRearBogeyPosition(getCurrentTickPosOrFake(), offset);
	}
	public PosRot predictRearBogeyPosition(TickPos pos, float offset) {
		MovementSimulator sim = new MovementSimulator(world, pos, this.getDefinition().getBogeyFront(), this.getDefinition().getBogeyRear());
		
		Vec3d rear = sim.rearBogeyPosition();
		Vec3d nextRear = rear;
		while (offset > 0) {
			nextRear = sim.nextPosition(nextRear, pos.rotationYaw+180, VecUtil.fromYaw(Math.min(0.1, offset), pos.rearYaw+180));
			offset -= 0.1;
		}
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		return new PosRot(nextRear.subtractReverse(pos.position), VecUtil.toYaw(rearDelta));
	}
}
