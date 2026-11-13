package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

public class VehicleRequestPacket extends Packet {
    @TagField
    private World world;
    @TagField
    private int minX, minZ, maxX, maxZ;

    public VehicleRequestPacket() {}

    public VehicleRequestPacket(World world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    @Override
    protected void handle() {
        WorldData data = WorldData.get(world);
        if (data != null) {
            data.handleVehicleRequest(getPlayer(), minX, minZ, maxX, maxZ);
        }
    }
}