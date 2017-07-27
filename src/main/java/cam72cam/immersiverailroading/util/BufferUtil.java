package cam72cam.immersiverailroading.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.Nullable;

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
	
	public static void writeUUID(ByteBuf buf, @Nullable UUID val) {
		if (val != null) {
			writeString(buf, val.toString());
		} else {
			writeString(buf, "NULLUUID");
		}
	}
	public static @Nullable UUID readUUID(ByteBuf buf) {
		String val = readString(buf);
		if (!val.equals("NULLUUID")) {
			return UUID.fromString(val);
		} else {
			return null;
		}
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
