package cam72cam.immersiverailroading.net.offline;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.graph.TrackMultiGeometrySegment;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

public class SwitchChangePacket extends Packet {
    @TagField
    private World world;
    @TagField
    private Vec3i pos;
    @TagField(typeHint = SwitchState.class)
    private SwitchState targetState;
    @TagField
    private boolean online;

    public SwitchChangePacket() {
        // Reflection
    }

    public SwitchChangePacket(World world, Vec3i pos, SwitchState targetState, boolean online) {
        this.world = world;
        this.pos = pos;
        this.targetState = targetState;
        this.online = online;
    }

    @Override
    protected void handle() {
        if (online) {
            world.forceLoadChunkAt(pos);

            TileRail tile = world.getBlockEntity(pos, cam72cam.immersiverailroading.tile.TileRail.class);

            if (tile != null) {
                tile.setSwitchForced(targetState);
                tile.markAllDirty();
            }
            return;
        }

        WorldData data = WorldData.get(world);
        if (data != null) data.updateSwitchState(pos, targetState, true, System.currentTimeMillis());
    }
}