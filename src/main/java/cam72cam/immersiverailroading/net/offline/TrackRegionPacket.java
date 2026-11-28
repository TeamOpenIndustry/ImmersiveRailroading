package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.TrackRegion;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;

public class TrackRegionPacket extends Packet {
    @TagField
    private World world;
    @TagField
    private long id;

    public TrackRegionPacket() {
        // Reflection
    }

    public TrackRegionPacket(World world, long id, TrackRegion region) {
        this.world = world;
        this.id = id;
        // This is stupidly inefficient
        // TODO LZ4 compress?
        this.raw = region == null ? new byte[0] : region.write().array();
    }

    @Override
    protected void handle() {
        WorldData data = WorldData.getOrCreate(world);
        if (data == null) {
            return;
        }

        // region become empty
        if (raw == null || raw.length == 0) {
            data.clientUnloadRegion(id);
            return;
        }

        TrackRegion region = new TrackRegion(id, ByteBuffer.wrap(raw), world);
        data.clientLoadRegion(id, region);
    }
}