package cam72cam.immersiverailroading.data;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.nio.ByteBuffer;

public class TrackInfo extends RailInfo {
    public final int id; // not serialized
    public final long region; // not serialized
    public final int parent_id;
    public final long parent_region;

    public TrackInfo(int id, RailInfo info, TrackInfo parent) {
        this(id, info, parent == null ? -1 : parent.id, parent == null ? 0 : parent.region);
    }
    public TrackInfo(int id, RailInfo info, int parent_id, long parent_region) {
        super(info);
        this.id = id;
        this.region = WorldData.vecToRegion(new Vec3i(placementInfo.placementPosition));
        this.parent_id = parent_id;
        this.parent_region = parent_region;
    }

    public static TrackInfo load(int id, ByteBuffer buffer) {
        byte peek = buffer.get();
        if (peek == -1) {
            return null;
        }

        SwitchState switchState = SwitchState.values()[peek];
        SwitchState switchForced = SwitchState.values()[buffer.get()];
        double tablePos = buffer.getDouble();
        RailSettings settings = loadRailSettings(buffer);
        PlacementInfo placementInfo = loadPlacementInfo(buffer);
        PlacementInfo customInfo = loadPlacementInfo(buffer);
        int parent_id = buffer.getInt();
        long parent_region = buffer.getLong();

        return new TrackInfo(id, new RailInfo(
                settings,
                placementInfo,
                customInfo,
                switchState,
                switchForced,
                tablePos
        ), parent_id, parent_region);
    }

    public static int sizeBytes(TrackInfo info) {
        if (info == null) {
            return 1;
        }
        return info.sizeBytes();
    }

    public int sizeBytes() {
        int bytes = 0;
        bytes += 1; // switchState
        bytes += 1; // switchForced
        bytes += Double.BYTES; // tablePos
        bytes += sizeRailSettings(settings);
        bytes += sizePlacementInfo(placementInfo);
        bytes += sizePlacementInfo(customInfo);
        bytes += Integer.BYTES; // parent_id
        bytes += Long.BYTES; // parent_region
        return bytes;
    }

    public static void save(TrackInfo info, ByteBuffer buffer) {
        if (info == null) {
            buffer.put((byte)-1);
            return;
        }
        info.save(buffer);
    }

    public void save(ByteBuffer buffer) {
        buffer.put((byte) switchState.ordinal());
        buffer.put((byte) switchForced.ordinal());
        buffer.putDouble(tablePos);
        saveRailSettings(settings, buffer);
        savePlacementInfo(placementInfo, buffer);
        savePlacementInfo(customInfo, buffer);
        buffer.putInt(parent_id);
        buffer.putLong(parent_region);
    }

    private static PlacementInfo loadPlacementInfo(ByteBuffer buffer) {
        byte peek = buffer.get();
        if (peek == -1) {
            return null;
        }
        TrackDirection direction = TrackDirection.values()[peek];
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        double z = buffer.getDouble();
        Vec3d placementPosition = new Vec3d(x, y, z);
        float yaw = buffer.getFloat();
        Vec3d control;
        if (buffer.get() == 1) {
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            control = new Vec3d(x, y, z);
        } else {
            control = null;
        }
        return new PlacementInfo(placementPosition, direction, yaw, control);
    }

    private static int sizePlacementInfo(PlacementInfo placementInfo) {
        int bytes = 0;
        bytes += 1; // direction
        bytes += 3 * Double.BYTES; // placementPosition
        bytes += Float.BYTES; // yaw
        bytes += 1; // hasControl
        if (placementInfo.control != null) {
            bytes += 3 * Double.BYTES; // control
        }
        return bytes;
    }

    private static void savePlacementInfo(PlacementInfo placementInfo, ByteBuffer buffer) {
        buffer.put((byte) placementInfo.direction.ordinal());
        buffer.putDouble(placementInfo.placementPosition.x);
        buffer.putDouble(placementInfo.placementPosition.y);
        buffer.putDouble(placementInfo.placementPosition.z);
        buffer.putFloat(placementInfo.yaw);
        if (placementInfo.control == null) {
            buffer.put((byte) 0);
        } else {
            buffer.put((byte) 1);
            buffer.putDouble(placementInfo.control.x);
            buffer.putDouble(placementInfo.control.y);
            buffer.putDouble(placementInfo.control.z);
        }
    }


    private static RailSettings loadRailSettings(ByteBuffer buffer) {
        float gauge = buffer.getFloat();
        TrackItems type = TrackItems.values()[buffer.getInt()];
        int length = buffer.getInt();
        float degrees = buffer.getFloat();
        float curvosity = buffer.getFloat();
        TrackPositionType posType = TrackPositionType.values()[buffer.get()];
        TrackSmoothing smoothing = TrackSmoothing.values()[buffer.get()];
        TrackDirection direction = TrackDirection.values()[buffer.get()];
        ItemStack railBed = Util.readItemStack(buffer);
        ItemStack railBedFill = Util.readItemStack(buffer);
        // TODO compress into single field?
        boolean isPreview = buffer.get() == 1;
        boolean isGradeCrossing = buffer.get() == 1;
        String track = Util.readString(buffer);
        return new RailSettings(
                Gauge.from(gauge),
                track,
                type,
                length,
                degrees,
                curvosity,
                posType,
                smoothing,
                direction,
                railBed,
                railBedFill,
                isPreview,
                isGradeCrossing
        );
    }


    private static int sizeRailSettings(RailSettings settings) {
        int bytes = 0;
        bytes += Float.BYTES; // gauge
        bytes += Integer.BYTES; // type
        bytes += Integer.BYTES; // length
        bytes += Float.BYTES; // degrees
        bytes += Float.BYTES; // curvosity
        bytes += 1; // posType
        bytes += 1; // smoothing
        bytes += 1; // direction
        bytes += Util.sizeItemStack(settings.railBed);
        bytes += Util.sizeItemStack(settings.railBedFill);
        bytes += 1; // isPreview
        bytes += 1; // isGradeCrossing
        bytes += Util.sizeString(settings.track);
        return bytes;
    }


    private static void saveRailSettings(RailSettings settings, ByteBuffer buffer) {
        buffer.putFloat((float) settings.gauge.value());
        buffer.putInt(settings.type.ordinal());
        buffer.putInt(settings.length);
        buffer.putFloat(settings.degrees);
        buffer.putFloat(settings.curvosity);
        buffer.put((byte) settings.posType.ordinal());
        buffer.put((byte) settings.smoothing.ordinal());
        buffer.put((byte) settings.direction.ordinal());
        Util.writeItemStack(settings.railBed, buffer);
        Util.writeItemStack(settings.railBedFill, buffer);
        buffer.put((byte) (settings.isPreview ? 1 : 0));
        buffer.put((byte) (settings.isGradeCrossing ? 1 : 0));
        Util.writeString(settings.track, buffer);
    }
}
