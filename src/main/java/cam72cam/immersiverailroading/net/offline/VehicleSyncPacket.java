package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VehicleSyncPacket extends Packet {
    private static final int ENTRY_BYTES = Long.BYTES * 2 + Double.BYTES * 3 + 1;

    @TagField
    private World world;

    public VehicleSyncPacket() {}

    public VehicleSyncPacket(World world, List<WorldData.MapVehicle> vehicles) {
        this.world = world;
        if (vehicles == null || vehicles.isEmpty()) {
            this.raw = new byte[0];
        } else {
            ByteBuffer buf = ByteBuffer.allocate(vehicles.size() * ENTRY_BYTES);
            for (WorldData.MapVehicle v : vehicles) {
                buf.putLong(v.id.getMostSignificantBits());
                buf.putLong(v.id.getLeastSignificantBits());
                buf.putDouble(v.pos.x);
                buf.putDouble(v.pos.y);
                buf.putDouble(v.pos.z);
                buf.put(v.locomotive ? (byte) 1 : (byte) 0);
            }
            this.raw = buf.array();
        }
    }

    @Override
    protected void handle() {
        WorldData data = WorldData.get(world);
        if (data != null) {
            if (raw == null || raw.length == 0) {
                data.updateClientVehicles(new ArrayList<>());
                return;
            }
            int count = raw.length / ENTRY_BYTES;
            List<WorldData.MapVehicle> vehicles = new ArrayList<>(count);
            ByteBuffer buf = ByteBuffer.wrap(raw);
            for (int i = 0; i < count; i++) {
                UUID id = new UUID(buf.getLong(), buf.getLong());
                Vec3d pos = new Vec3d(buf.getDouble(), buf.getDouble(), buf.getDouble());
                vehicles.add(new WorldData.MapVehicle(id, pos, buf.get() != 0));
            }
            data.updateClientVehicles(vehicles);
        }
    }
}
