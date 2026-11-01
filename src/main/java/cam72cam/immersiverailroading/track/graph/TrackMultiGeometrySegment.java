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
    private final long regionPos;
    private final Vec3i regionBlockPos;

    private long buildTime;
    protected final List<Map<Gauge, TrackSingleGeometrySegment>> paths;
    private int switchState;
    private double tablePos;
    private long lastOnlineBuildTime;

    public final TrackItems type;

    private List<Vec3i> cachedPositions;

    public Vec3i getAbsPos() {
        return TrackRegionUtil.toBlockPos(regionPos, regionBlockPos);
    }

    public List<Map<Gauge, TrackSingleGeometrySegment>> getPaths() {
        return paths;
    }

    public TrackMultiGeometrySegment(TileRail trackBlock, World world) {
        this.regionPos = TrackRegionUtil.vecToRegion(trackBlock.getPos());
        this.regionBlockPos = TrackRegionUtil.toRegionBlockPos(trackBlock.getPos());

        buildTime = trackBlock.getBuildTimeMs();
        lastOnlineBuildTime = buildTime;

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
        tablePos = trackBlock.info.tablePos;
    }

    public int getSwitchState() {
        return switchState;
    }

    public SwitchState getSwitchStateEnum() {
        return SwitchState.values()[this.switchState];
    }

    public SwitchState nextSwitchState() {
        return SwitchState.values()[( getSwitchStateEnum().ordinal() + 1 ) % SwitchState.values().length];
    }

    protected void updateSwitchSate(SwitchState switchState, boolean offline, long currentTime) {
        this.switchState = switchState.ordinal();
        if(!offline) {
            this.buildTime = currentTime;
            this.lastOnlineBuildTime = currentTime;
        } else {
            this.buildTime = currentTime;
        }
    }

    protected void updateTablePos(double tablePos, boolean offline, long currentTime) {
        if(tablePos == this.tablePos) return;
        this.tablePos = tablePos;
        for(Map<Gauge, TrackSingleGeometrySegment> branch : paths) {
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                TrackSingleGeometrySegment single = entry.getValue();
                single.updateForTable(tablePos, type);
            }
        }
        if(!offline) {
            this.buildTime = currentTime;
            this.lastOnlineBuildTime = currentTime;
        } else {
            this.buildTime = currentTime;
        }
    }

    public TrackMultiGeometrySegment(ByteBuffer buffer, World world, long regionPos, Vec3i regionBlockPos) {
        this.regionPos = regionPos;
        this.regionBlockPos = regionBlockPos;

        int version = buffer.getInt();
        if (version != 1) {
            throw new RuntimeException(String.format("Invalid multi track geometry segment data version %d", version));
        }

        buildTime = buffer.getLong();
        lastOnlineBuildTime = buffer.getLong();
        switchState = buffer.getInt();
        tablePos = buffer.getDouble();
        type = TrackItems.byOrder(buffer.getInt());

        paths = new ArrayList<>();
        int branchCount = buffer.getInt();
        for(int i = 0; i < branchCount; i ++) {
            int segmentCount = buffer.getInt();
            Map<Gauge, TrackSingleGeometrySegment> branch = new HashMap<>();
            for(int j = 0; j < segmentCount; j++) {
                Gauge gauge = Gauge.from(buffer.getDouble());
                TrackSingleGeometrySegment single = new TrackSingleGeometrySegment(buffer, world);
                single.updateForTable(tablePos, type);
                branch.put(gauge, single);
            }
            paths.add(branch);
        }
    }

    public int sizeBytes() {
        int bytes = 0;

        bytes += Integer.BYTES;    // version
        bytes += Long.BYTES;       // buildTime
        bytes += Long.BYTES;       // lastOnlineBuildTime
        bytes += Integer.BYTES;    // switchState
        bytes += Double.BYTES;     // tablePos
        bytes += Integer.BYTES;    // type

        bytes += Integer.BYTES;    // branch count
        for(int i = 0; i < paths.size(); i ++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = paths.get(i);
            bytes += Integer.BYTES; // segment count
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                bytes += Double.BYTES;
                bytes += entry.getValue().sizeBytes();
            }
        }

        return bytes;
    }

    public void write(ByteBuffer buffer) {
        buffer.putInt(1);              // version
        buffer.putLong(buildTime);           // buildTime
        buffer.putLong(lastOnlineBuildTime); // buildTime
        buffer.putInt(switchState);          // switchState
        buffer.putDouble(tablePos);          // tablePos
        buffer.putInt(type.getOrder());      // type

        buffer.putInt(paths.size());
        for(int i = 0; i < paths.size(); i ++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = paths.get(i);
            buffer.putInt(branch.size());
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                buffer.putDouble(entry.getKey().value());
                entry.getValue().write(buffer);
            }
        }
    }

    public List<Vec3i> getPositionsCache() {
        if (cachedPositions != null) return cachedPositions;
        List<Vec3i> positionCache = new ArrayList<>();
        for(Map<Gauge, TrackSingleGeometrySegment> branch : paths) {
            for(Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                TrackSingleGeometrySegment single = entry.getValue();
                Vec3i offsetToParent = single.getAbsPos().subtract(this.getAbsPos());
                for (Vec3i p : single.positionsCache) {
                    positionCache.add(offsetToParent.add(p));
                }
            }
        }
        cachedPositions = positionCache;
        return cachedPositions;
    }
}