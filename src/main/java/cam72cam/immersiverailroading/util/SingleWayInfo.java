package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.*;

import java.util.Arrays;
import java.util.function.Consumer;

@TagMapped(SingleWayInfo.TagMapper.class)
public class SingleWayInfo {
    public final RailSettings settings;
    public final PlacementInfo placementInfo;
    public final PlacementInfo customInfo;
    public final int wayOrder;

    public SingleWayInfo(RailSettings settings,PlacementInfo placementInfo, PlacementInfo customInfo,int wayOrder) {
        if (customInfo == null) {
            customInfo = placementInfo;
            if (settings.type == TrackItems.SLOPE) {
                customInfo = customInfo.offset(new Vec3i(0,1,0));
            }
        }

        this.settings = settings;
        this.placementInfo = placementInfo;
        this.customInfo = customInfo;
        this.wayOrder = wayOrder;
    }

    public static final RailSettings defaultSettings = new RailSettings(
            Gauge.standard(),
            "default",
            TrackItems.TURN,TrackItems.TURN,
            15,
            90,
            1f,
            TrackPositionType.FIXED, TrackSmoothing.BOTH,
            0f,0f,
            0f,0f,
            true,-1,
            TrackDirection.RIGHT,
            ItemStack.EMPTY, ItemStack.EMPTY,
            true,
            false,
            1,
            1
    );
    public static final PlacementInfo defaultPos = new PlacementInfo(
            new Vec3d(0.5, 0, 0.5), TrackDirection.LEFT, 0, null
    );

    public static class Mutable {
        @TagField("settings")
        public RailSettings settings;
        @TagField("placement")
        public PlacementInfo placementInfo;
        @TagField("custom")
        public PlacementInfo customInfo;
        @TagField("wayOrder")
        public int wayOrder;

        public Mutable(SingleWayInfo info) {
            this.settings = info.settings;
            this.placementInfo = info.placementInfo;
            this.customInfo = info.customInfo;
            this.wayOrder = info.wayOrder;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            TagSerializer.deserialize(data, this);
        }

        public SingleWayInfo immutable() {
            return new SingleWayInfo(
                    settings,
                    placementInfo,
                    customInfo,
                    wayOrder
            );
        }
    }

    public SingleWayInfo.Mutable mutable() {
        return new SingleWayInfo.Mutable(this);
    }
    static class TagMapper implements cam72cam.mod.serialization.TagMapper<SingleWayInfo> {
        @Override
        public TagAccessor<SingleWayInfo> apply(Class<SingleWayInfo> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (d, o) -> {
                        TagCompound target = new TagCompound();
                        try {
                            TagSerializer.serialize(target, o.mutable());
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                        d.set(fieldName, target);
                    },
                    d -> {
                        try {
                            return new SingleWayInfo.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }

    public SingleWayInfo with(Consumer<SingleWayInfo.Mutable> mod) {
        SingleWayInfo.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    @Override
    public String toString() {
        Object[] props = new Object [] {
                this.wayOrder,
                this.settings.type,
                this.settings.length,
                this.settings.degrees,
                this.settings.curvosity,
                this.settings.railBed,
                this.settings.gauge,
                this.settings.track,
                this.settings.smoothing,
                this.settings.pitchStart,
                this.settings.pitchEnd,
                this.settings.isForward,
                this.settings.farRadius,
                this.settings.isGradeCrossing,
                this.placementInfo.yaw,
                this.placementInfo.direction,
                this.customInfo.yaw,
                this.customInfo.direction,
        };
        String id = Arrays.toString(props);
        if (!placementInfo.placementPosition.equals(customInfo.placementPosition) || this.settings.posType != TrackPositionType.FIXED) {
            id += placementInfo.placementPosition.subtract(customInfo.placementPosition);
        }
        if (placementInfo.control != null) {
            id += placementInfo.control;
        }
        if (customInfo.control != null) {
            id += customInfo.control;
        }
        return id;
    }
}
