package cam72cam.immersiverailroading.data;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Region {
    private final Map<Vec3i, TrackBlock> trackBlocks;
    final List<TrackInfo> trackInfos;
    boolean needsWriteToDisk;
    boolean modifiedSinceLastTick;

    public Region() {
        trackBlocks = new HashMap<>();
        trackInfos = new ArrayList<>();
    }

    public Region(ByteBuffer buffer) {
        int version = buffer.getInt();
        if (version != 1) {
            throw new RuntimeException(String.format("Invalid track block data version %d", version));
        }

        int size = buffer.getInt();
        trackBlocks = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int x = buffer.getInt();
            int y = buffer.getInt();
            int z = buffer.getInt();
            trackBlocks.put(new Vec3i(x, y, z), new TrackBlock(buffer));
        }

        size = buffer.getInt();

        trackInfos = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            trackInfos.add(i, TrackInfo.load(i, buffer));
        }
    }

    private int sizeBytes() {
        int bytes = 0;
        bytes += Integer.BYTES; // version
        bytes += Integer.BYTES; // trackBlocks.size()
        for (TrackBlock value : trackBlocks.values()) {
            bytes += Integer.BYTES * 3; // pos.xyz
            bytes += value.sizeBytes(); // trackBlock.write()
        }
        bytes += Integer.BYTES; // trackInfos.size()
        for (TrackInfo trackInfo : trackInfos) {
            bytes += TrackInfo.sizeBytes(trackInfo);
        }
        return bytes;
    }

    public ByteBuffer write() {
        // Could be faster with a CoW, not sure how long this blocking is
        synchronized (trackBlocks) {
            ByteBuffer buffer = ByteBuffer.allocate(sizeBytes());

            buffer.putInt(1); // version
            buffer.putInt(trackBlocks.size());
            for (Map.Entry<Vec3i, TrackBlock> entry : trackBlocks.entrySet()) {
                Vec3i pos = entry.getKey();
                buffer.putInt(pos.x);
                buffer.putInt(pos.y);
                buffer.putInt(pos.z);
                entry.getValue().write(buffer);
            }
            buffer.putInt(trackInfos.size());
            for (TrackInfo trackInfo : trackInfos) {
                TrackInfo.save(trackInfo, buffer);
            }
            return buffer;
        }
    }

    public TrackBlock getTrackBlock(Vec3i pos) {
        synchronized (trackBlocks) {
            return trackBlocks.get(pos);
        }
    }

    public void setTrackBlock(Vec3i pos, TrackBlock block) {
        synchronized (trackBlocks) {
            trackBlocks.put(pos, block);
            needsWriteToDisk = true;
            modifiedSinceLastTick = true;
        }
    }

    public TrackInfo getTrackInfo(int info_id) {
        synchronized (trackInfos) {
            return trackInfos.get(info_id);
        }
    }

    public void updateTrackInfo(TrackInfo info) {
        synchronized (trackInfos) {
            trackInfos.set(info.id, info);
            needsWriteToDisk = true;
            modifiedSinceLastTick = true;
        }
    }

    public TrackInfo allocateTrackInfo(RailInfo info, TrackInfo parent) {
        synchronized (trackInfos) {
            TrackInfo allocated = new TrackInfo(trackInfos.size(), info, parent);
            trackInfos.add(allocated);
            needsWriteToDisk = true;
            modifiedSinceLastTick = true;
            return allocated;
        }
    }

    public List<TrackInfo> getTrackInfos() {
        return trackInfos;
    }
}
