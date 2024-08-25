package cam72cam.immersiverailroading.data;

import java.nio.ByteBuffer;

public class TrackBlock {
    TrackBlock replaced;
    long info_region;
    int info_id;
    float bedHeight;
    float railHeight;
    int snowLayers;

    public TrackBlock(ByteBuffer buffer) {
        if (buffer.get() == 1) {
            replaced = new TrackBlock(buffer);
        }
        info_region = buffer.getLong();
        info_id = buffer.getInt();
        bedHeight = buffer.getFloat();
        railHeight = buffer.getFloat();
        snowLayers = buffer.getInt();
    }

    public TrackBlock(TrackInfo info, float railHeight, float bedHeight, int snowLayers, TrackBlock replaced) {
        this.info_id = info.id;
        this.info_region = info.region;
        this.railHeight = railHeight;
        this.bedHeight = bedHeight;
        this.replaced = replaced;
        this.snowLayers = snowLayers;
    }

    public void write(ByteBuffer buffer) {
        if (replaced == null) {
            buffer.put((byte)0);
        } else {
            buffer.put((byte)1);
            replaced.write(buffer);
        }
        buffer.putLong(info_region);
        buffer.putInt(info_id);
        buffer.putFloat(bedHeight);
        buffer.putFloat(railHeight);
        buffer.putInt(snowLayers);
    }

    public int sizeBytes() {
        int bytes = 0;
        bytes += 1; // replaced marker
        if (replaced != null) {
            bytes += replaced.sizeBytes();
        }
        bytes += Long.BYTES; // info_region
        bytes += Integer.BYTES; // info_id
        bytes += Float.BYTES; // bedHeight
        bytes += Float.BYTES; // railHeight
        bytes += Integer.BYTES; // snowLayers
        return bytes;
    }
}
