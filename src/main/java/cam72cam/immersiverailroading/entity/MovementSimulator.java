package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.tile.TileRailGag;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.SwitchUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MovementSimulator {
	private World world;
	private TickPos position;
	private float bogeyFrontOffset;
	private float bogeyRearOffset;

	public MovementSimulator(World world, TickPos startPos, float bogeyFrontOffset, float bogeyRearOffset) {
		this.world = world;
		this.position = startPos.clone(); //  We may modify it for convenience later on
		this.bogeyFrontOffset = bogeyFrontOffset;
		this.bogeyRearOffset = bogeyRearOffset;
	}
	
	public TickPos nextPosition(double moveDistance) {
		position.tickID += 1;
		position.isOffTrack = false;
		TickPos origPosition = position.clone();

		if (Math.abs(moveDistance) < 0.001) {
			position.speed = Speed.ZERO;
			return position;
		}
		
		position.speed = Speed.fromMinecraft(Math.abs(moveDistance));

		position.isReverse = moveDistance < 0;

		Vec3d front = frontBogeyPosition();
		Vec3d rear = rearBogeyPosition();
		
		if (position.isReverse) {
			moveDistance = -moveDistance;
			position.frontYaw += 180;
			position.rearYaw += 180;
			position.rotationYaw += 180;
			//position.rotationPitch = -position.rotationPitch;
			position.rotationYaw = (position.rotationYaw + 360f) % 360f;
			position.frontYaw = (position.frontYaw + 360f) % 360f;
			position.rearYaw = (position.rearYaw + 360f) % 360f;
		}

		
		
		Vec3d nextFront = nextPosition(front, position.rotationYaw, VecUtil.fromYaw(moveDistance, position.frontYaw));
		Vec3d nextRear = nextPosition(rear, position.rotationYaw, VecUtil.fromYaw(moveDistance, position.rearYaw));
		if (nextFront.equals(front) || nextRear == rear) {
			origPosition.speed = Speed.fromMetric(0);
			if (position.isOffTrack) {
				origPosition.isOffTrack = true;
			}
			return origPosition;
		}
		Vec3d frontDelta = front.subtractReverse(nextFront);
		Vec3d rearDelta = rear.subtractReverse(nextRear);
		position.frontYaw = VecUtil.toYaw(frontDelta);
		position.rearYaw = VecUtil.toYaw(rearDelta);

		Vec3d currCenter = VecUtil.between(front, rear);
		Vec3d nextCenter = VecUtil.between(nextFront, nextRear);
		Vec3d deltaCenter = currCenter.subtractReverse(nextCenter);

		Vec3d bogeySkew = nextRear.subtractReverse(nextFront);
		position.rotationYaw = VecUtil.toYaw(bogeySkew);
		position.rotationPitch = (float) Math.toDegrees(MathHelper.atan2(bogeySkew.y, nextRear.distanceTo(nextFront)));

		if (position.isReverse) {
			position.frontYaw += 180;
			position.rearYaw += 180;
			//rotationYaw += 180;
			//position.rotationPitch = -position.rotationPitch;
			position.rotationYaw = (position.rotationYaw + 360f) % 360f;
			position.frontYaw = (position.frontYaw + 360f) % 360f;
			position.rearYaw = (position.rearYaw + 360f) % 360f;
		}
		
		

		position.position = position.position.add(deltaCenter);
		if (world.isAirBlock(new BlockPos(position.position))) {
			// Fall
			position.position = position.position.addVector(0, -0.1, 0);
		}
		
		return position;
	}

	public Vec3d frontBogeyPosition() {
		return VecUtil.fromYawPitch(bogeyFrontOffset, position.rotationYaw, position.rotationPitch).add(position.position);
	}

	public Vec3d rearBogeyPosition() {
		return VecUtil.fromYawPitch(bogeyRearOffset, position.rotationYaw, position.rotationPitch).add(position.position);
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
		if (parent == null) {
			return null;
		}
		
		TileRail super_parent = parent.getParentTile();
		if (SwitchUtil.getSwitchState(parent, position) == SwitchState.STRAIGHT) {
			parent = super_parent;
		}
		
		return parent;
	}
	
	private TileRail fuzzyRailPosition(Vec3d position) {
		TileRail rail = railFromPosition(position);
		if (rail != null) {
			return rail;
		}
		
		// Try a smidge higher
		// We get some wobble on the top of slopes, this corrects for imperfect precision
		rail = railFromPosition(position.addVector(0, 0.4, 0));
		if (rail != null) {
			return rail;
		}
		rail = railFromPosition(position.addVector(0, -0.4, 0));
		if (rail != null) {
			return rail;
		}
		return null;
	}
	
	private TileRail closeEnoughRailPosition(Vec3d position, float trainYaw) {
		TileRail rail = fuzzyRailPosition(position);
		if (rail != null) {
			return rail;
		}
		rail = fuzzyRailPosition(position.add(VecUtil.fromYaw(1, trainYaw)));
		if (rail != null) {
			return rail;
		}
		rail = fuzzyRailPosition(position.add(VecUtil.fromYaw(-1, trainYaw)));
		if (rail != null) {
			return rail;
		}
		return null;
	}

	public Vec3d nextPosition(Vec3d position, float trainYaw, Vec3d delta) {
		TileRail rail = closeEnoughRailPosition(position, trainYaw);
		
		if (rail == null) {
			if (!world.isRemote) {
				//System.out.println("WARNING OFF TRACK!!!");
				//System.out.println(position);
				//System.out.println(world.getBlockState(new BlockPos(position)).getBlock().getLocalizedName());
				this.position.isOffTrack = true;
				return position;
			} else {
				return position.add(delta);
			}
		}

		double distance = delta.lengthVector();

		if (rail.getType().isTurn()) {
			// Relative position to the curve center
			Vec3d posDelta = rail.getCenter().subtractReverse(position);
			// Calculate the angle (rad) for the current position is
			double posRelYaw = MathHelper.atan2(posDelta.x, -posDelta.z);
			// Hack the radius
			double radius = rail.getRadius() - 0.5;
			// Calculate the angle delta in rad (radians are awesome)
			double yawDelt = distance / radius;

			// Calculate the original next position (won't be accurate, but we
			// use it as an estimate)
			Vec3d nextPos = position.add(delta);

//			for (int i = 0; i < 90; i++) {
//				Vec3d check = rail.getCenter().addVector(Math.sin(posRelYaw + Math.toRadians(i)) * radius, 0,
//						-Math.cos(posRelYaw + Math.toRadians(i)) * radius);
//				world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, check.x, check.y, check.z, 0, 0, 0);
//			}

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
		} else if (rail.getType() == TrackItems.CROSSING) {
			delta = VecUtil.fromYaw(distance, EnumFacing.fromAngle(trainYaw).getHorizontalAngle());
			return position.add(delta);
		} else {
			// delta should be in the direction of rotationYaw instead of front or rear
			// since large changes can occur if the train is way off center
			delta = VecUtil.fromYaw(distance, trainYaw);
			/*
			// Check that we are not moving in the wrong axis along a track
			if (EnumFacing.fromAngle(trainYaw).getAxis() != rail.getFacing().getAxis() && rail.getType() != TrackItems.CROSSING) {
				if (!world.isRemote) {
					System.out.println("Wrong track direction");
					startPos.setDead();
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
			
			Vec3d outPosition;
			if (possiblePositive.distanceTo(position.add(delta)) < possibleNegative.distanceTo(position.add(delta))) {
				outPosition = possiblePositive;
			} else {
				outPosition = possibleNegative;
			}

			// Update y position
			TileRailBase directRail = directRailFromPosition(outPosition);
			if (directRail != null) {
				outPosition = new Vec3d(outPosition.x, directRail.getPos().getY() + directRail.getHeight(), outPosition.z);
			} else {
				outPosition = new Vec3d(outPosition.x, position.y, outPosition.z);
			}
			
			return outPosition;
		}
	}
}
