package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.math.Vec3i;

public final class TrackNodeId {
    public final long regionPos;           // 所属 region
    public final Vec3i regionBlockPos;     // region 内哪个 trackBlock
    public final int pathIndex;            // trackBlock.paths 里的下标
    public final Gauge gauge;              // paths.get(pathIndex) 里的 key
    public final int pointIndex;           // single.pointsCache 里的下标

    public final Vec3i absPos;

    private final int cachedHash;

    public TrackNodeId(long regionPos, Vec3i regionBlockPos, int pathIndex, Gauge gauge, int pointIndex) {
        this.regionPos = regionPos;
        this.regionBlockPos = regionBlockPos;
        absPos = TrackRegionUtil.toBlockPos(regionPos, regionBlockPos);
        this.pathIndex = pathIndex;
        this.gauge = gauge;
        this.pointIndex = pointIndex;
        this.cachedHash = computeHash();
    }

    private int computeHash() {
        int h = Long.hashCode(regionPos);
        h = 31 * h + regionBlockPos.x;
        h = 31 * h + regionBlockPos.y;
        h = 31 * h + regionBlockPos.z;
        h = 31 * h + pathIndex;
        h = 31 * h + gauge.hashCode();
        h = 31 * h + pointIndex;
        return h;
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TrackNodeId)) return false;
        TrackNodeId o = (TrackNodeId) obj;
        return regionPos == o.regionPos
                && regionBlockPos.equals(o.regionBlockPos)
                && pathIndex == o.pathIndex
                && gauge == o.gauge
                && pointIndex == o.pointIndex;
    }
}
