package cam72cam.immersiverailroading.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.entity.EntityRidableRollingStock.StaticPassenger;
import cam72cam.immersiverailroading.library.ItemComponentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.common.network.ByteBufUtils;

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

	public static void writeVec3i(ByteBuf buf, Vec3i pos) {
		buf.writeInt(pos.getX());
		buf.writeInt(pos.getY());
		buf.writeInt(pos.getZ());
	}
	
	public static Vec3i readVec3i(ByteBuf buf) {
		return new Vec3i(buf.readInt(), buf.readInt(), buf.readInt());
	}
	
	public static void writeItemComponentTypes(ByteBuf buf, List<ItemComponentType> items) {
		buf.writeInt(items.size());
		for (ItemComponentType item : items) {
			buf.writeInt(item.ordinal());
		}
	}
	
	public static List<ItemComponentType> readItemComponentTypes(ByteBuf buf) {
		List<ItemComponentType> items = new ArrayList<ItemComponentType>();
		
		int count = buf.readInt();
		for (int i = 0; i < count; i ++) {
			items.add(ItemComponentType.values()[buf.readInt()]);
		}
		
		return items;
	}

	public static void writeStaticPassengers(ByteBuf buffer, List<StaticPassenger> staticPassengers) {
		buffer.writeInt(staticPassengers.size());
		for (StaticPassenger pass : staticPassengers) {
			ByteBufUtils.writeTag(buffer, pass.writeNBT());
		}
	}
	
	public static List<StaticPassenger> readStaticPassengers(ByteBuf buffer) {
		List<StaticPassenger> staticPassengers = new ArrayList<StaticPassenger>();
		int count = buffer.readInt();
		for (int i = 0; i < count; i++) {
			staticPassengers.add(new StaticPassenger(ByteBufUtils.readTag(buffer)));
		}
		return staticPassengers;
	}
}
