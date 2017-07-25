package cam72cam.immersiverailroading.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;

public class BufferUtil {
	
	public static void writeString(ByteBuf buf, String val) {
		buf.writeInt(val.getBytes(StandardCharsets.UTF_8).length);
		buf.writeBytes(val.getBytes(StandardCharsets.UTF_8));
	}
	public static String readString(ByteBuf buf) {
		byte[] defBytes = new byte[buf.readInt()];
		buf.readBytes(defBytes);
		return new String(defBytes, StandardCharsets.UTF_8);
	}
	
	public static void writeUUID(ByteBuf buf, UUID val) {
		writeString(buf, val.toString());
	}
	public static UUID readUUID(ByteBuf buf) {
		return UUID.fromString(readString(buf));
	}
	
	public static void writeVec3d(ByteBuf buf, Vec3d val) {
		buf.writeDouble(val.x);
		buf.writeDouble(val.y);
		buf.writeDouble(val.z);
	}
	public static Vec3d readVec3d(ByteBuf buf) {
		return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}
}
