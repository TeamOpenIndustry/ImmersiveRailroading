package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.TrackDirection;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
	
	public PlacementInfo(ItemStack stack, float yawHead, BlockPos pos, float hitX, float hitY, float hitZ) {
		yawHead = ((- yawHead % 360) + 360) % 360;
		this.yaw = ((int)((yawHead + 90/8f) * 4)) / 90 * 90 / 4f;

		RailSettings settings = ItemTrackBlueprint.settings(stack);
		TrackDirection direction = settings.direction;
		if (direction == TrackDirection.NONE) {
			direction = (yawHead % 90 < 45) ? TrackDirection.RIGHT : TrackDirection.LEFT;
		}

		int quarter = rotationQuarter();

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

		this.placementPosition = new Vec3d(pos).addVector(hitX, 0, hitZ);
		this.direction = direction;
		this.control = null;
	}

	public PlacementInfo(NBTTagCompound nbt) {
		this(nbt, BlockPos.ORIGIN);
	}
	
	public PlacementInfo(NBTTagCompound nbt, BlockPos offset) {
		this.placementPosition = NBTUtil.nbtToVec3d(nbt.getCompoundTag("placementPosition")).addVector(offset.getX(), offset.getY(), offset.getZ());
		this.direction = TrackDirection.values()[nbt.getInteger("direction")];
		if (nbt.hasKey("yaw")) {
			this.yaw = nbt.getFloat("yaw");
		} else {
			int rotationQuarter = nbt.getInteger("rotationQuarter");
			EnumFacing facing = EnumFacing.getFront(nbt.getByte("facing"));
			float facingAngle = 180 - facing.getHorizontalAngle();
			float rotAngle = rotationQuarter/4f*90;
			if (direction != TrackDirection.RIGHT) {
				rotAngle = -rotAngle;
			}
			this.yaw = facingAngle + rotAngle;
		}
		if (nbt.hasKey("magnitude")) {
			// TODO: LEGACY REMOVE in 1.6
			this.control = placementPosition.add(VecUtil.fromYaw(nbt.getDouble("magnitude"), yaw));
		} else if (nbt.hasKey("control")) {
			this.control = NBTUtil.nbtToVec3d(nbt.getCompoundTag("control")).addVector(offset.getX(), offset.getY(), offset.getZ());
		} else {
			this.control = null;
		}
	}
	
	public NBTTagCompound toNBT() {
		return toNBT(BlockPos.ORIGIN);
	}
	
	public NBTTagCompound toNBT(BlockPos offset) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("placementPosition", NBTUtil.vec3dToNBT(placementPosition.subtract(offset.getX(), offset.getY(), offset.getZ())));
		nbt.setFloat("yaw", yaw);
		nbt.setInteger("direction", direction.ordinal());
		if (control != null) {
			nbt.setTag("control", NBTUtil.vec3dToNBT(control.subtract(offset.getX(), offset.getY(), offset.getZ())));
		}
		return nbt;
	}

	public EnumFacing facing() {
		return EnumFacing.fromAngle(180-yaw);
	}

	public int rotationQuarter() {
		return (int)((yaw % 90) *4/90);
	}

	public float partialAngle() {
		return yaw % 90;
	}
}
