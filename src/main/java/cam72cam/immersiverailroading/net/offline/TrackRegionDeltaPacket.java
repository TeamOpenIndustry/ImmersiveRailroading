package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.track.graph.TrackRegionUtil;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.List;

public class TrackRegionDeltaPacket extends Packet {
    @TagField
    private World world;
    @TagField
    private long id;

    public TrackRegionDeltaPacket() {
        // Reflection
    }

    public TrackRegionDeltaPacket(World world, long id, List<WorldData.DeltaOp> ops) {
        this.world = world;
        this.id = id;
        this.raw = WorldData.serializeDeltaOps(ops);
    }

    @Override
    protected void handle() {
        WorldData data = WorldData.getOrCreate(world);
        if (data == null) return;

        if (raw == null || raw.length == 0) return;

        // if region not loaded, should we just return?
        if (!data.hasRegion(id)) return;

        List<WorldData.DeltaOp> ops = WorldData.deserializeDeltaOps(raw, world, id);
        for (WorldData.DeltaOp op : ops) {
            Vec3i absPos = TrackRegionUtil.toBlockPos(id, op.regionBlockPos);
            switch (op.kind) {
                case PUT -> data.setTrackBlock(absPos, op.block);
                case REMOVE -> data.removeTrackBlock(absPos);
                case SWITCH_STATE -> data.clientApplySwitchStateDelta(id, op.regionBlockPos, op.switchState, op.offline, op.currentTime);
            }
        }
    }
}