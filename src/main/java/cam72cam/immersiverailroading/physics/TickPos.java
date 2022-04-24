package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;

import java.util.List;

public class TickPos {
	@TagField("tickID")
	public int tickID;
	@TagField("speed")
	public Speed speed;
	@TagField("offTrack")
	public boolean isOffTrack;
	
	//Vec3d frontPosition;
	//Vec3d backPosition;
	@TagField("pos")
	public Vec3d position;
	@TagField("frontYaw")
	public float frontYaw;
	@TagField("rearYaw")
	public float rearYaw;
	@TagField("rotationYaw")
	public float rotationYaw;
	@TagField("rotationPitch")
	public float rotationPitch;

	public TickPos(int tickPosID, Speed speed, Vec3d position, float frontYaw, float rearYaw, float rotationYaw, float rotationPitch, boolean isOffTrack) {
		this.tickID = tickPosID;
		this.speed = speed;
		this.isOffTrack = isOffTrack;
		this.position = position;
		this.frontYaw = frontYaw;
		this.rearYaw = rearYaw;
		this.rotationYaw = rotationYaw;
		this.rotationPitch = rotationPitch;
	}

	private TickPos(TagCompound tag) {
		try {
			TagSerializer.deserialize(tag, this);
		} catch (SerializationException e) {
			ImmersiveRailroading.catching(e);
		}
	}

	private TagCompound toTag() {
		TagCompound tag = new TagCompound();
		try {
			TagSerializer.serialize(tag, this);
		} catch (SerializationException e) {
			ImmersiveRailroading.catching(e);
		}
		return tag;
	}

	@Override
	public TickPos clone() {
		return new TickPos(tickID, speed, position, frontYaw, rearYaw, rotationYaw, rotationPitch, isOffTrack);
	}
}