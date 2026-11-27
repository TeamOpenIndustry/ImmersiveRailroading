package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrackRegion {
    private static final int VERSION = 1;

    public final long regionPos;// addition, not necessary

    public final Map<Vec3i, TrackMultiGeometrySegment> trackBlocks;// fake final
    private volatile long buildTime;

    private volatile boolean needsWriteToDisk;

    public boolean needsWriteToDisk() {
        return needsWriteToDisk;
    }

    public void markDirty() {
        this.needsWriteToDisk = true;
    }

    public void clearDirty() {
        this.needsWriteToDisk = false;
    }

    public long getBuildTime() {
        return buildTime;
    }

    public TrackRegion(long regionPos) {
        this.regionPos = regionPos;
        this.trackBlocks = new HashMap<>();
    }

    public Vec3i toBlockPos(Vec3i regionBlockPos) {
        int rx = (int) (regionPos >> 32);
        int rz = (int) regionPos;
        return new Vec3i(
                (rx << 9) + regionBlockPos.x,
                regionBlockPos.y,
                (rz << 9) + regionBlockPos.z
        );
    }

    public TrackRegion(long regionPos, ByteBuffer buffer, World world) {
        this.regionPos = regionPos;
        int version = buffer.getInt();
        if (version != VERSION) {
            throw new RuntimeException(String.format("Invalid track block data version %d", version));
        }
        buildTime = buffer.getLong();

        int size = buffer.getInt();
        trackBlocks = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int x = buffer.getInt();
            int y = buffer.getInt();
            int z = buffer.getInt();
            Vec3i regionBlockPos = new Vec3i(x, y, z);
            TrackMultiGeometrySegment segment = new TrackMultiGeometrySegment(buffer, world, regionPos, regionBlockPos);
            trackBlocks.put(regionBlockPos, segment);
        }
    }

    private int sizeBytes() {
        int bytes = 0;
        bytes += Integer.BYTES; // version
        bytes += Long.BYTES;    // buildTime
        bytes += Integer.BYTES; // trackBlocks.size()
        for (TrackMultiGeometrySegment value : trackBlocks.values()) {
            bytes += Integer.BYTES * 3; // pos.xyz
            bytes += value.sizeBytes(); // trackBlock.write()
        }

        return bytes;
    }

    public ByteBuffer write() {
        // Could be faster with a CoW, not sure how long this blocking is
        synchronized (trackBlocks) {
            ByteBuffer buffer = ByteBuffer.allocate(sizeBytes());

            buffer.putInt(VERSION);
            buffer.putLong(buildTime);
            buffer.putInt(trackBlocks.size());
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> entry : trackBlocks.entrySet()) {
                Vec3i pos = entry.getKey();// rel pos in region
                buffer.putInt(pos.x);
                buffer.putInt(pos.y);
                buffer.putInt(pos.z);
                entry.getValue().write(buffer);
            }
            return buffer;
        }
    }

    public TrackMultiGeometrySegment getTrackBlock(Vec3i pos) {
        synchronized (trackBlocks) {
            return trackBlocks.get(TrackRegionUtil.toRegionBlockPos(pos));
        }
    }

    public boolean isEmpty() {
        synchronized (trackBlocks) {
            return trackBlocks.isEmpty();
        }
    }

    public boolean removeTrackBlock(Vec3i pos) {
        synchronized (trackBlocks) {
            Vec3i relPos = TrackRegionUtil.toRegionBlockPos(pos);
            TrackMultiGeometrySegment removed = trackBlocks.remove(relPos);
            if (removed == null) {
                return false;
            }
            updateBuildTime(removed);
            needsWriteToDisk = true;
            return true;
        }
    }

    public void setTrackBlock(Vec3i pos, TrackMultiGeometrySegment block) {
        synchronized (trackBlocks) {
            updateBuildTime(block);
            Vec3i relBlockPos = TrackRegionUtil.toRegionBlockPos(pos);
            trackBlocks.put(relBlockPos, block);
            needsWriteToDisk = true;
        }
    }

    public void updateBuildTime(TrackMultiGeometrySegment block) {
        synchronized (trackBlocks) {
            long blockBuildTime = block.getBuildTime();
            if (blockBuildTime < buildTime) {
                ImmersiveRailroading.debug(
                        "Track block at %s has buildTime %s, older than region buildTime %s",
                        block.getAbsPos(),
                        formatBuildTime(blockBuildTime),
                        formatBuildTime(buildTime)
                );
            }
            buildTime = Math.max(blockBuildTime, buildTime);
        }
    }

    private static String formatBuildTime(long buildTime) {
        return String.format(
                "%d (%s)",
                buildTime,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(buildTime))
        );
    }
}
