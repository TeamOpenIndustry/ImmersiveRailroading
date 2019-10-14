package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.TagCompound;

public class TickPos {
	public int tickID;
	public Speed speed;
	public boolean isOffTrack;
	
	//Vec3d frontPosition;
	//Vec3d backPosition;
	public Vec3d position;
	
	public float frontYaw;
	public float rearYaw;
	public float rotationYaw;
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

	public TickPos(TagCompound tag) {
		tickID = tag.getInteger("tickID");
		speed = Speed.fromMetric(tag.getDouble("speed"));
		isOffTrack = tag.getBoolean("offTrack");
		position = tag.getVec3d("pos");

		frontYaw = tag.getFloat("frontYaw");
		rearYaw = tag.getFloat("rearYaw");
		rotationYaw = tag.getFloat("rotationYaw");
		rotationPitch = tag.getFloat("rotationPitch");
	}

	public TagCompound toTag() {
		TagCompound tag = new TagCompound();

		tag.setInteger("tickID", tickID);
		tag.setDouble("speed", speed.metric());
		tag.setBoolean("offTrack", isOffTrack);
		tag.setVec3d("pos", position);
		tag.setFloat("frontYaw", frontYaw);
		tag.setFloat("rearYaw", rearYaw);
		tag.setFloat("rotationYaw", rotationYaw);
		tag.setFloat("rotationPitch", rotationPitch);

		return tag;
	}

	@Override
	public TickPos clone() {
		return new TickPos(tickID, speed, position, frontYaw, rearYaw, rotationYaw, rotationPitch, isOffTrack);
	}
}