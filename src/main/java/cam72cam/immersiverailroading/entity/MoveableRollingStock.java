package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailGag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class MoveableRollingStock extends EntityRollingStock {

	private Float frontYaw;
	private Float rearYaw;

	public MoveableRollingStock(World world, String defID) {
		super(world, defID);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setFloat("frontYaw", frontYaw);
		nbttagcompound.setFloat("rearYaw", rearYaw);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		frontYaw = nbttagcompound.getFloat("frontYaw");
		rearYaw = nbttagcompound.getFloat("rearYaw");
	}

	public void moveRollingStock(double moveDistance) {
		if (frontYaw == null) {
			frontYaw = rotationYaw;
		}
		if (rearYaw == null) {
			rearYaw = rotationYaw;
		}

		Vec3d front = frontBogeyPosition();
		Vec3d rear = rearBogeyPosition();
		Vec3d nextFront = nextPosition(front, this.frontYaw, nextMovement(this.frontYaw, moveDistance));
		Vec3d nextRear = nextPosition(rear, this.rearYaw, nextMovement(this.rearYaw, moveDistance));
		Vec3d frontDelta = front.subtractReverse(nextFront);
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		frontYaw = (float) Math.toDegrees(Math.atan2(-frontDelta.x, frontDelta.z));
		rearYaw = (float) Math.toDegrees(Math.atan2(-rearDelta.x, rearDelta.z));
		frontYaw = (frontYaw + 360f) % 360f;
		rearYaw = (rearYaw + 360f) % 360f;

		Vec3d currCenter = between(front, rear);
		Vec3d nextCenter = between(nextFront, nextRear);
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = nextRear.subtractReverse(nextFront);

		this.prevRotationYaw = rotationYaw;

		this.rotationYaw = (float) Math.toDegrees(Math.atan2(-bogeySkew.x, bogeySkew.z));
		this.rotationYaw = (this.rotationYaw + 360f) % 360f;

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
	}

	private Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}

	protected abstract float frontBogeyOffset();

	protected abstract float rearBogeyOffset();

	private Vec3d frontBogeyPosition() {
		// Vec3d front = new Vec3d(0, 0, frontBogeyOffset());
		// front = front.rotateYaw((float) Math.toRadians(this.rotationYaw));
		Vec3d front = new Vec3d(-Math.sin(Math.toRadians(this.rotationYaw)) * frontBogeyOffset(), 0,
				Math.cos(Math.toRadians(this.rotationYaw)) * frontBogeyOffset());
		return front.addVector(posX, posY, posZ);
	}

	private Vec3d rearBogeyPosition() {
		// Vec3d rear = new Vec3d(0, 0, rearBogeyOffset());
		// rear = rear.rotateYaw((float) Math.toRadians(this.rotationYaw));
		Vec3d rear = new Vec3d(-Math.sin(Math.toRadians(this.rotationYaw)) * rearBogeyOffset(), 0,
				Math.cos(Math.toRadians(this.rotationYaw)) * rearBogeyOffset());
		return rear.addVector(posX, posY, posZ);
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
		double x = -Math.sin(Math.toRadians(yaw));
		double z = Math.cos(Math.toRadians(yaw));
		return new Vec3d(x * d, 0, z * d);
	}

	private Vec3d nextPosition(Vec3d position, float yaw, Vec3d delta) {
		TileRail rail = railFromPosition(position);

		if (rail == null) {
			System.out.println("WARNING OFF TRACK!!!");
			System.out.println(new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z)));
			System.out.println(world.getBlockState(new BlockPos(position)).getBlock().getLocalizedName());
			// if (this.ticksExisted > 100) { // HACK
			this.setDead();
			// }
			return position;
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
