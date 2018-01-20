package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.physics.MovementSimulator;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.RedstoneUtil;
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
	public float distanceTraveled = 0;
	public float renderDistanceTraveled = 0;
	public double tickPosID = 0;
	private Speed currentSpeed;
	public List<TickPos> positions = new ArrayList<TickPos>();
	private AxisAlignedBB boundingBox;
	private double[][] heightMapCache;

	public EntityMoveableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		frontYaw = BufferUtil.readFloat(additionalData);
		rearYaw = BufferUtil.readFloat(additionalData);
		tickPosID = additionalData.readInt();
		
		positions = new ArrayList<TickPos>();
		
		for (int numPositions =additionalData.readInt(); numPositions > 0; numPositions --) {
			TickPos pos = new TickPos();
			pos.read(additionalData);
			positions.add(pos);
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		BufferUtil.writeFloat(buffer, frontYaw);
		BufferUtil.writeFloat(buffer, rearYaw);
		buffer.writeInt((int)tickPosID);
		
		buffer.writeInt(positions.size());
		for (TickPos pos : positions ) {
			pos.write(buffer);
		}
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
		this.positions.add(new TickPos((int)this.tickPosID, this.getCurrentSpeed(), this.getPositionVector(), this.rotationYaw, this.rotationYaw, this.rotationYaw, this.rotationPitch, false));
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
		return this.getEntityBoundingBox().contract(0, 0.5, 0).offset(0, 0.5, 0);
	}
	
	public void clearHeightMap() {
		this.heightMapCache = null;
		this.boundingBox = null;
	}
	
	private double[][] getHeightMap() {
		if (this.heightMapCache == null) {
			this.heightMapCache = this.getDefinition().createHeightMap(this);
		}
		return this.heightMapCache;
	}

	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		if (this.boundingBox == null) {
			this.boundingBox = this.getDefinition().getBounds(this, this.gauge).withHeightMap(this.getHeightMap());
		}
		return this.boundingBox;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
		AxisAlignedBB bb = this.getEntityBoundingBox();
        return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
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
		if (newPositions.size() != 0) {
	    	this.clearPositionCache();
			if (this.ticksExisted > 5) {
				ImmersiveRailroading.proxy.addTickMetric(tickPosID - newPositions.get(0).tickID);
			}
			this.tickPosID = newPositions.get(0).tickID;
		}
		this.positions = newPositions;
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
	
	private double skewScalar(double curr, double next) {
		if (world.isRemote) {
			return curr + (next - curr) * ImmersiveRailroading.proxy.serverTicksPerClientTick();
		}
		return next;
	}
	private float skewScalar(float curr, float next) {
		if (world.isRemote) {
			return curr + (next - curr) * (float)ImmersiveRailroading.proxy.serverTicksPerClientTick();
		}
		return next;
	}
	
	private float fixAngleInterp(float curr, float next) {
		if (curr - next > 180) {
			curr -= 360;
    	}
    	if (next - curr > 180) {
    		curr += 360;
    	}
    	return curr;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		this.tickPosID += world.isRemote ? ImmersiveRailroading.proxy.serverTicksPerClientTick() : 1;
		
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
		

	    this.posX = skewScalar(this.posX, currentPos.position.x);
	    this.posY = skewScalar(this.posY, currentPos.position.y);
	    this.posZ = skewScalar(this.posZ, currentPos.position.z);

	    if (world.isRemote) {
	    	this.prevRotationYaw = fixAngleInterp(this.prevRotationYaw, currentPos.rotationYaw);
	    	this.rotationYaw = fixAngleInterp(this.rotationYaw, currentPos.rotationYaw);
	    	this.frontYaw = fixAngleInterp(this.frontYaw == null ? this.rotationYaw : this.frontYaw, currentPos.frontYaw);
	    	this.rearYaw = fixAngleInterp(this.rearYaw == null ? this.rotationYaw : this.rearYaw, currentPos.rearYaw);
	    }
		    
	    this.rotationYaw = skewScalar(this.rotationYaw, currentPos.rotationYaw);
	    this.rotationPitch = skewScalar(this.rotationPitch, currentPos.rotationPitch);
	    this.frontYaw = skewScalar(this.frontYaw == null ? this.rotationYaw : this.frontYaw, currentPos.frontYaw);
	    this.rearYaw = skewScalar(this.rearYaw == null ? this.rotationYaw : this.rearYaw, currentPos.rearYaw);
	    
	    this.currentSpeed = currentPos.speed;
		distanceTraveled = skewScalar(distanceTraveled, distanceTraveled + (float)this.currentSpeed.minecraft());
		
	    this.motionX = this.posX - this.prevPosX;
	    this.motionY = this.posY - this.prevPosY;
	    this.motionZ = this.posZ - this.prevPosZ;

	    if (Math.abs(this.motionX) + Math.abs(this.motionY) + Math.abs(this.motionZ) > 0.001 ) {
	    	this.clearPositionCache();
	    }

		List<Entity> entitiesWithin = world.getEntitiesWithinAABB(Entity.class, this.getCollisionBoundingBox().offset(0, -0.5, 0));
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
				entity.attackEntityFrom((new DamageSource("immersiverailroading:hitByTrain")).setDamageBypassesArmor(), (float) speedDamage);
			}
		}

		// Riding on top of cars
		AxisAlignedBB bb = this.getCollisionBoundingBox();
		bb = bb.expand(0, gauge.scale(), 0);
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
		if (!world.isRemote && this.ticksExisted % 5 == 0 && Config.TrainsBreakBlocks && Math.abs(this.getCurrentSpeed().metric()) > 0.5) {
			bb = this.getCollisionBoundingBox();
			
			for (Vec3d pos : this.getDefinition().getBlocksInBounds(gauge)) {
				pos = VecUtil.rotateYaw(pos, this.rotationYaw);
				pos = pos.add(this.getPositionVector());
				BlockPos bp = new BlockPos(pos);
				IBlockState state = world.getBlockState(bp);
				if (state.getBlock() != Blocks.AIR) {
					if (!BlockUtil.isRail(world, bp)) {
						if (bb.contains(pos)) { // This is slow, do it as little as possible
							if (!BlockUtil.isRail(world, bp.up())) {
								world.destroyBlock(bp, true);										
							}
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

	protected void clearPositionCache() {
		this.boundingBox = null;
	}

	public TickPos moveRollingStock(double moveDistance, int lastTickID) {
		TickPos lastPos = this.getTickPos(lastTickID);
		return new MovementSimulator(world, lastPos, this.getDefinition().getBogeyFront(gauge), this.getDefinition().getBogeyRear(gauge), gauge.value()).nextPosition(moveDistance);
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
		return new TickPos(0, Speed.fromMetric(0), this.getPositionVector(), this.getFrontYaw(), this.getRearYaw(), this.rotationYaw, this.rotationPitch, false);
	}
	
	public PosRot predictFrontBogeyPosition(float offset) {		
		return predictFrontBogeyPosition(getCurrentTickPosOrFake(), offset);
	}
	public PosRot predictFrontBogeyPosition(TickPos pos, float offset) {		
		MovementSimulator sim = new MovementSimulator(world, pos, this.getDefinition().getBogeyFront(gauge), this.getDefinition().getBogeyRear(gauge), gauge.value());
		
		Vec3d front = sim.frontBogeyPosition();
		Vec3d nextFront = front;
		while (offset > 0) {
			nextFront = sim.nextPosition(nextFront, pos.rotationYaw, pos.frontYaw, Math.min(0.1, offset));
			offset -= 0.1;
		}
		Vec3d frontDelta = front.subtractReverse(nextFront);
		return new PosRot(nextFront.subtractReverse(pos.position), VecUtil.toYaw(frontDelta));
	}
	
	public PosRot predictRearBogeyPosition(float offset) {		
		return predictRearBogeyPosition(getCurrentTickPosOrFake(), offset);
	}
	public PosRot predictRearBogeyPosition(TickPos pos, float offset) {
		MovementSimulator sim = new MovementSimulator(world, pos, this.getDefinition().getBogeyFront(gauge), this.getDefinition().getBogeyRear(gauge), gauge.value());
		
		Vec3d rear = sim.rearBogeyPosition();
		Vec3d nextRear = rear;
		while (offset > 0) {
			nextRear = sim.nextPosition(nextRear, pos.rotationYaw+180, pos.rearYaw+180, Math.min(0.1, offset));
			offset -= 0.1;
		}
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		return new PosRot(nextRear.subtractReverse(pos.position), VecUtil.toYaw(rearDelta));
	}

	public int getSpeedRetarderSlowdown(TickPos latest) {
		int over = 0;
		double y = -1;
		for (Vec3d pos : this.getDefinition().getBlocksInBounds(gauge)) {
			if (y == -1) {
				y = pos.y;
			}
			if (pos.y != y) {
				break;
			}
			pos = VecUtil.rotateYaw(pos, this.rotationYaw);
			pos = pos.add(latest.position);
			BlockPos bp = new BlockPos(pos);
			bp = bp.down();
			IBlockState state = world.getBlockState(bp);
			if (state.getBlock() != Blocks.AIR) {
				if (BlockUtil.isIRRail(world, bp)) {
					TileRailBase te = TileRailBase.get(world, bp);
					if (te != null && te.getAugment() == Augment.SPEED_RETARDER) {
						TileRail parent = te.getParentTile();
						if (parent != null) {
							int max = 0;
							BlockPos tmpPos = bp;
							while(true) {
								tmpPos = tmpPos.offset(parent.getFacing());
								TileRailBase tmp = TileRailBase.get(world, tmpPos);
								if (tmp != null && tmp.getAugment() == Augment.SPEED_RETARDER) {
									max = Math.max(max, RedstoneUtil.getPower(world, tmpPos));
								} else {
									break;
								}
							}
							while(true) {
								tmpPos = tmpPos.offset(parent.getFacing().getOpposite());
								TileRailBase tmp = TileRailBase.get(world, tmpPos);
								if (tmp != null && tmp.getAugment() == Augment.SPEED_RETARDER) {
									max = Math.max(max, RedstoneUtil.getPower(world, tmpPos));
								} else {
									break;
								}
							}
							over += max;
						}
					}
				}
			}
		}
		return over;
	}
}
