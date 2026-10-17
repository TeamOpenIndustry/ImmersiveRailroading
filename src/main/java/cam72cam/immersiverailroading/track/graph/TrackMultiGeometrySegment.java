package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackMultiGeometrySegment {
    private final long regionPos;// addition, not necessary
    private final Vec3i regionBlockPos;// addition, not necessary

    private final long buildTime;
    public final List<Map<Gauge, TrackSingleGeometrySegment>> paths;
    protected int switchState;

    public final TrackItems type; // for table and switch

    public Vec3i getBlockPos() {
        return TrackRegionUtil.toBlockPos(regionPos, regionBlockPos);
    }

    public TrackMultiGeometrySegment(TileRail trackBlock, World world) {
        this.regionPos = TrackRegionUtil.vecToRegion(trackBlock.getPos());
        this.regionBlockPos = TrackRegionUtil.toRegionBlockPos(trackBlock.getPos());

        buildTime = trackBlock.getBuildTimeMs();

        TrackSingleGeometrySegment segment = new TrackSingleGeometrySegment(trackBlock, world, 0, trackBlock.info.settings.gauge);
        paths = new ArrayList<>();
        Map<Gauge, TrackSingleGeometrySegment> branch = new HashMap<>();
        branch.put(trackBlock.info.settings.gauge, segment);
        paths.add(branch);

        if(trackBlock.info.settings.type == TrackItems.SWITCH && trackBlock.subInfos != null && !trackBlock.subInfos.isEmpty()) {
            TrackSingleGeometrySegment turn = new TrackSingleGeometrySegment(trackBlock, world, 1, trackBlock.info.settings.gauge);
            Map<Gauge, TrackSingleGeometrySegment> turnBranch = new HashMap<>();
            turnBranch.put(trackBlock.info.settings.gauge, turn);
            paths.add(turnBranch);
        }

        type = trackBlock.info.settings.type;
        switchState = trackBlock.info.switchState.ordinal();
    }

    public void updateSwitchSate(SwitchState switchState) {
        this.switchState = switchState.ordinal();
    }

    public void updateTableState() {
        //todo
    }

    public TrackMultiGeometrySegment(ByteBuffer buffer, World world, long regionPos, Vec3i regionBlockPos) {
        this.regionPos = regionPos;
        this.regionBlockPos = regionBlockPos;

        int version = buffer.getInt(); // version
        if (version != 1) {
            throw new RuntimeException(String.format("Invalid multi track geometry segment data version %d", version));
        }

        buildTime = buffer.getLong(); // buildTime
        switchState = buffer.getInt(); // switch state
        type = TrackItems.byOrder(buffer.getInt()); // type

        paths = new ArrayList<>();
        int branchCount = buffer.getInt(); // branch count
        for(int i = 0; i < branchCount; i ++) {
            int segmentCount = buffer.getInt(); // segment count
            Map<Gauge, TrackSingleGeometrySegment> branch = new HashMap<>();
            for(int j = 0; j < segmentCount; j++) {
                Gauge gauge = Gauge.from(buffer.getDouble()); // segment gauge
                branch.put(gauge, new TrackSingleGeometrySegment(buffer, world, regionPos, regionBlockPos)); // single segment
            }
            paths.add(branch);
        }
    }

    public int sizeBytes() {
        int bytes = 0;

        bytes += Integer.BYTES; // version
        bytes += Long.BYTES; // buildTime
        bytes += Integer.BYTES; // switch state
        bytes += Integer.BYTES; // type

        bytes += Integer.BYTES; // branch count
        for(int i = 0; i < paths.size(); i ++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = paths.get(i);
            bytes += Integer.BYTES; // segment count
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                bytes += Double.BYTES; // segment gauge
                bytes += entry.getValue().sizeBytes(); // single segment
            }
        }

        return bytes;
    }

    public void write(ByteBuffer buffer) {
        buffer.putInt(1); // version
        buffer.putLong(buildTime); // buildTime
        buffer.putInt(switchState); // switch state
        buffer.putInt(type.getOrder()); // type

        buffer.putInt(paths.size()); // branch count
        for(int i = 0; i < paths.size(); i ++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = paths.get(i);
            buffer.putInt(branch.size()); // segment count
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                buffer.putDouble(entry.getKey().value()); // segment gauge
                entry.getValue().write(buffer); // single segment
            }
        }
    }

    public List<Vec3i> getPositionsCache() {
        List<Vec3i> positionCache = new ArrayList<>();
        for(Map<Gauge, TrackSingleGeometrySegment> branch : paths) {
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                positionCache.addAll(entry.getValue().positionsCache);
            }
        }
        return positionCache;
    }
}
