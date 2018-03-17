package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.util.BufferUtil;
import cam72cam.immersiverailroading.util.Speed;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;

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

	
	public TickPos() {
	}
	
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

	@Override
	public TickPos clone() {
		return new TickPos(tickID, speed, position, frontYaw, rearYaw, rotationYaw, rotationPitch, isOffTrack);
	}

	public void write(ByteBuf buf) {
		buf.writeInt(tickID);
		buf.writeFloat((float) speed.metric());
		buf.writeBoolean(isOffTrack);
		
		//BufferUtil.writeVec3d(buf, frontPosition);
		//BufferUtil.writeVec3d(buf, backPosition);
		BufferUtil.writeVec3d(buf, position);
		
		buf.writeFloat(frontYaw);
		buf.writeFloat(rearYaw);
		buf.writeFloat(rotationYaw);
		buf.writeFloat(rotationPitch);
	}
	
	public void read(ByteBuf buf) {
		tickID = buf.readInt();
		speed = Speed.fromMetric(buf.readFloat());
		isOffTrack = buf.readBoolean();
		
		//frontPosition = BufferUtil.readVec3d(buf);
		//backPosition = BufferUtil.readVec3d(buf);
		position = BufferUtil.readVec3d(buf);
		
		frontYaw = buf.readFloat();
		rearYaw = buf.readFloat();
		rotationYaw = buf.readFloat();
		rotationPitch = buf.readFloat();
	}
}