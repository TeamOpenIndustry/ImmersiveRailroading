package cam72cam.immersiverailroading.net;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.track.graph.TrackRegion;
import cam72cam.immersiverailroading.track.graph.TrackRegionUtil;
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
            synchronized (data.regions) {
                data.regions.remove(id);
                ImmersiveRailroading.info("region %s,%s unloaded", TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));
            }
            return;
        }

        TrackRegion region = new TrackRegion(id, ByteBuffer.wrap(raw), world);
        synchronized (data.regions) {
            data.regions.put(id, region);
            ImmersiveRailroading.info("region %s,%s loaded", TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));
        }

        data.rebuildTopologyAround(id);
    }
}