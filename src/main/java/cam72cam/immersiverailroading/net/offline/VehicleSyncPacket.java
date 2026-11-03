package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VehicleSyncPacket extends Packet {
    @TagField
    private World world;

    public VehicleSyncPacket() {}

    public VehicleSyncPacket(World world, List<Vec3d> vehicles) {
        this.world = world;
        if (vehicles == null || vehicles.isEmpty()) {
            this.raw = new byte[0];
        } else {
            // only vehicle pos data yet
            ByteBuffer buf = ByteBuffer.allocate(vehicles.size() * 24);
            for (Vec3d v : vehicles) {
                buf.putDouble(v.x);
                buf.putDouble(v.y);
                buf.putDouble(v.z);
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
            int count = raw.length / 24;
            List<Vec3d> vehicles = new ArrayList<>(count);
            ByteBuffer buf = ByteBuffer.wrap(raw);
            for (int i = 0; i < count; i++) {
                vehicles.add(new Vec3d(buf.getDouble(), buf.getDouble(), buf.getDouble()));
            }
            data.updateClientVehicles(vehicles);
        }
    }
}