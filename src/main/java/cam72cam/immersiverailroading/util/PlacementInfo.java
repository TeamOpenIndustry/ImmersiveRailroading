package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.serialization.TagCompound;

public class PlacementInfo {
	public final Vec3d placementPosition; // relative
	public final TrackDirection direction;
	public final float yaw;
	public final Vec3d control;

	public PlacementInfo(Vec3d placementPosition, TrackDirection direction, float yaw, Vec3d control) {
		this.placementPosition = placementPosition;
		this.direction = direction;
		this.yaw = yaw;
		this.control = control;
	}

	public static int segmentation() {
		return Math.min(32, Math.max(1, Config.ConfigBalance.AnglePlacementSegmentation));
	}
	
	public PlacementInfo(ItemStack stack, float yawHead, Vec3i pos, Vec3d hit) {
		yawHead = ((- yawHead % 360) + 360) % 360;
		this.yaw = ((int)((yawHead + 90/(segmentation() * 2f)) * segmentation())) / 90 * 90 / (segmentation() * 1f);

		RailSettings settings = RailSettings.from(stack);
		TrackDirection direction = settings.direction;
		if (direction == TrackDirection.NONE) {
			direction = (yawHead % 90 < 45) ? TrackDirection.RIGHT : TrackDirection.LEFT;
		}

		int quarter = rotationQuarter();

		double hitX = hit.x;
		double hitZ = hit.z;

		switch(settings.posType) {
		case FIXED:
			hitX = 0.5f;
			hitZ = 0.5f;
			break;
		case PIXELS:
			hitX = ((int)(hitX * 16)) / 16f;
			hitZ = ((int)(hitZ * 16)) / 16f;
			break;
		case PIXELS_LOCKED:
			hitX = ((int)(hitX * 16)) / 16f;
			hitZ = ((int)(hitZ * 16)) / 16f;

			if (quarter != 0) {
				break;
			}

			switch (facing()) {
			case EAST:
			case WEST:
				hitZ = 0.5f;
				break;
			case NORTH:
			case SOUTH:
				hitX = 0.5f;
				break;
			default:
				break;
			}
			break;
		case SMOOTH:
			// NOP
			break;
		case SMOOTH_LOCKED:
			if (quarter != 0) {
				break;
			}

			switch (facing()) {
			case EAST:
			case WEST:
				hitZ = 0.5f;
				break;
			case NORTH:
			case SOUTH:
				hitX = 0.5f;
				break;
			default:
				break;
			}
			break;
		}

		this.placementPosition = new Vec3d(pos).add(hitX, 0, hitZ);
		this.direction = direction;
		this.control = null;
	}

	public PlacementInfo(TagCompound nbt) {
		this(nbt, Vec3i.ZERO);
	}
	
	public PlacementInfo(TagCompound nbt, Vec3i offset) {
		this.placementPosition = nbt.getVec3d("placementPosition").add(offset);
		this.direction = TrackDirection.values()[nbt.getInteger("direction")];
		if (nbt.hasKey("yaw")) {
			this.yaw = nbt.getFloat("yaw");
		} else {
			int rotationQuarter = nbt.getInteger("rotationQuarter");
			Facing facing = Facing.from(nbt.getByte("facing"));
			float facingAngle = 180 - facing.getHorizontalAngle();
			float rotAngle = rotationQuarter/4f*90;
			if (direction != TrackDirection.RIGHT) {
				rotAngle = -rotAngle;
			}
			this.yaw = facingAngle + rotAngle;
		}
		if (nbt.hasKey("control")) {
			this.control = nbt.getVec3d("control").add(offset);
		} else {
			this.control = null;
		}
	}

	public TagCompound toNBT() {
		return toNBT(Vec3i.ZERO);
	}
	
	public TagCompound toNBT(Vec3i offset) {
		TagCompound nbt = new TagCompound();
		nbt.setVec3d("placementPosition", placementPosition.subtract(offset));
		nbt.setFloat("yaw", yaw);
		nbt.setInteger("direction", direction.ordinal());
		if (control != null) {
			nbt.setVec3d("control", control.subtract(offset));
		}
		return nbt;
	}

	public Facing facing() {
		return Facing.fromAngle(180-yaw);
	}

	public int rotationQuarter() {
		return (int)((yaw % 90) * segmentation() /90);
	}

	public float partialAngle() {
		return yaw % 90;
	}
}
