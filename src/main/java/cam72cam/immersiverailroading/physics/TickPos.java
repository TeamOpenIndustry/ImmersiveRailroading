package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.physics.SimulationState;
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

	public TickPos(SimulationState state) {
		this.tickID = state.tickID;
		this.speed = Speed.fromMinecraft(state.velocity);
		this.isOffTrack = false;
		this.position = state.position;
		this.rotationYaw = state.yaw;
		this.frontYaw = state.yawFront;
		this.rearYaw = state.yawRear;
		this.rotationPitch = state.pitch;
	}

	private TickPos(TagCompound tag) {
		try {
			TagSerializer.deserialize(tag, this);
		} catch (SerializationException e) {
			ImmersiveRailroading.catching(e);
		}
	}

	private static double skewScalar(double curr, double next, float ratio) {
		return curr + (next - curr) * ratio;
	}

	private static float skewAngle(float curr, float next, float ratio) {
		if (curr - next > 180) {
			curr -= 360;
		}
		if (next - curr > 180) {
			curr += 360;
		}

		return curr + (next - curr) * ratio;
	}

	public static TickPos skew(TickPos current, TickPos next, double tick) {
		float ratio = (float) (tick % 1);
		return new TickPos(
				current.tickID,
				current.speed,
				new Vec3d(
						skewScalar(current.position.x, next.position.x, ratio),
						skewScalar(current.position.y, next.position.y, ratio),
						skewScalar(current.position.z, next.position.z, ratio)
				),
				skewAngle(current.frontYaw, next.frontYaw, ratio),
				skewAngle(current.rearYaw, next.rearYaw, ratio),
				skewAngle(current.rotationYaw, next.rotationYaw, ratio),
				skewAngle(current.rotationPitch, next.rotationPitch, ratio),
				current.isOffTrack
		);
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

    public static class ListTagMapper implements TagMapper<List<TickPos>> {
        @Override
        public TagAccessor<List<TickPos>> apply(Class<List<TickPos>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (data, positions) -> data.setList(fieldName, positions, TickPos::toTag),
                    data -> data.getList(fieldName, TickPos::new)
            );
        }
    }
}