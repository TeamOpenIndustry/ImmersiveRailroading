package cam72cam.immersiverailroading.util;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

	public static void writeFloat(ByteBuf buf, @Nullable Float frontYaw) {
		if (frontYaw != null) {
			writeString(buf, frontYaw.toString());
		} else {
			writeString(buf, "NULLFLOAT");
		}
	}

	public static @Nullable Float readFloat(ByteBuf buf) {
		String val = readString(buf);
		if (!val.equals("NULLFLOAT")) {
			return Float.parseFloat(val);
		} else {
			return null;
		}
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

	public static void writePlayerPositions(ByteBuf buf, Map<UUID, Vec3d> passengerPositions) {
		buf.writeInt(passengerPositions.size());
		for (UUID entry : passengerPositions.keySet()) {
			writeUUID(buf, entry);
			writeVec3d(buf, passengerPositions.get(entry));
		}
	}

	public static Map<UUID, Vec3d> readPlayerPositions(ByteBuf buf) {
		HashMap<UUID, Vec3d> passengerPositions = new HashMap<UUID, Vec3d>();

		for (int itemCount = buf.readInt(); itemCount > 0; itemCount--) {
			UUID id = BufferUtil.readUUID(buf);
			Vec3d pos = BufferUtil.readVec3d(buf);
			passengerPositions.put(id, pos);
		}

		return passengerPositions;
	}
}
