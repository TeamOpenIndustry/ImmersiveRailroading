package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackPositionType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PlacementInfo {
	public final Vec3d placementPosition; // relative
	public final int rotationQuarter;
	public final TrackDirection direction;
	public final EnumFacing facing;
	
	public PlacementInfo(Vec3d placementPosition, int rotationQuarter, TrackDirection direction, EnumFacing facing) {
		this.placementPosition = placementPosition;
		this.rotationQuarter = rotationQuarter;
		this.direction = direction;
		this.facing = facing;
	}

	public PlacementInfo(ItemStack stack, float yawHead, BlockPos pos, float hitX, float hitY, float hitZ) {
		RailSettings settings = ItemTrackBlueprint.settings(stack);
		TrackDirection direction = settings.direction;
		int quarter = 0;
		
		yawHead = yawHead % 360 + 360;
		if (direction == TrackDirection.NONE) {
			direction = (yawHead % 90 < 45) ? TrackDirection.LEFT : TrackDirection.RIGHT;
		}
		//quarter = MathHelper.floor((yawHead % 90f) /(90)*4);
		float yawPartial = (yawHead+3600) % 90f;
		if (direction == TrackDirection.RIGHT) {
			yawPartial = 90-yawPartial;
		}
		if (yawPartial < 90.0/8*1) {
			quarter = 0;
		} else if (yawPartial < 90.0/8*3) {
			quarter = 1;
		} else if (yawPartial < 90.0/8*5) {
			quarter = 2;
		} else if (yawPartial < 90.0/8*7){
			quarter = 3;
		} else {
			quarter = 0;
			if (direction == TrackDirection.RIGHT) {
				yawHead -= 90;
			} else {
				yawHead += 90;
			}
		}
		
		//facing = EnumFacing.fromAngle(yawHead);
		if (direction == TrackDirection.RIGHT) {
			facing = EnumFacing.fromAngle(yawHead + 45);
		} else {
			facing = EnumFacing.fromAngle(yawHead - 45);
		}
		
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
			
			switch (facing) {
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
			
			switch (facing) {
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
		this.rotationQuarter = quarter;
		this.direction = direction;
	}

	public PlacementInfo(NBTTagCompound nbt) {
		this(nbt, BlockPos.ORIGIN);
	}
	
	public PlacementInfo(NBTTagCompound nbt, BlockPos offset) {
		this.placementPosition = NBTUtil.nbtToVec3d(nbt.getCompoundTag("placementPosition")).addVector(offset.getX(), offset.getY(), offset.getZ());
		this.rotationQuarter = nbt.getInteger("rotationQuarter");
		this.direction = TrackDirection.values()[nbt.getInteger("direction")];
		this.facing = EnumFacing.getFront(nbt.getByte("facing"));
	}
	
	public NBTTagCompound toNBT() {
		return toNBT(BlockPos.ORIGIN);
	}
	
	public NBTTagCompound toNBT(BlockPos offset) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("placementPosition", NBTUtil.vec3dToNBT(placementPosition.subtract(offset.getX(), offset.getY(), offset.getZ())));
		nbt.setInteger("rotationQuarter", rotationQuarter);
		nbt.setInteger("direction", direction.ordinal());
		nbt.setByte("facing", (byte) facing.getIndex());
		return nbt;
	}
}
