package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.*;

public class RailSettings {
    @TagField(value = "gauge")
    public final Gauge gauge;
    @TagField("type")
    public final TrackItems type;
    @TagField("length")
    public final int length;
    @TagField(value = "degrees", mapper = DegreesMapper.class)
    public final float degrees;
    @TagField("curvosity")
    public final float curvosity;
    @TagField("pos_type")
    public final TrackPositionType posType;
    @TagField(value = "smoothing", mapper = SmoothingMapper.class)
    public final TrackSmoothing smoothing;
    @TagField("direction")
    public final TrackDirection direction;
    @TagField("bedItem")
    public final ItemStack railBed;
    @TagField("bedFill")
    public final ItemStack railBedFill;
    @TagField("isPreview")
    public final boolean isPreview;
    @TagField("isGradeCrossing")
    public final boolean isGradeCrossing;
    @TagField("track")
    public final String track;

    public RailSettings(Gauge gauge, String track, TrackItems type, int length, float degrees, float curvosity, TrackPositionType posType, TrackSmoothing smoothing, TrackDirection direction, ItemStack railBed, ItemStack railBedFill, boolean isPreview, boolean isGradeCrossing) {
        this.gauge = gauge;
        this.track = track;
        this.type = type;
        this.length = length;
        this.degrees = degrees;
        this.posType = posType;
        this.smoothing = smoothing;
        this.direction = direction;
        this.railBed = railBed;
        this.railBedFill = railBedFill;
        this.isPreview = isPreview;
        this.isGradeCrossing = isGradeCrossing;
        this.curvosity = curvosity;
    }

    private RailSettings() {
        // Serialization
        gauge = Gauge.from(Gauge.STANDARD);
        type = TrackItems.STRAIGHT;
        track = "default";
        length = 10;
        degrees = 90;
        posType = TrackPositionType.FIXED;
        smoothing = TrackSmoothing.BOTH;
        direction = TrackDirection.NONE;
        railBed = ItemStack.EMPTY;
        railBedFill = ItemStack.EMPTY;
        isPreview = false;
        isGradeCrossing = false;
        curvosity = 1;
    }

    public void write(ItemStack stack) {
        TagCompound data = new TagCompound();
        try {
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        stack.setTagCompound(data);
    }

    public static RailSettings from(ItemStack stack) {
        RailSettings res = new RailSettings();
        try {
            TagSerializer.deserialize(stack.getTagCompound(), res);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        return res;
    }

    public Builder builder() {
        return new Builder(this);
    }

    private static class DegreesMapper implements TagMapper<Float> {
        @Override
        public TagAccessor<Float> apply(Class<Float> type, String fieldName, TagField tag) {
            return new TagAccessor<Float>(
                    (d, o) -> d.setFloat(fieldName, o),
                    d -> d.hasKey(fieldName) ? d.getFloat(fieldName) :
                            d.hasKey("quarters") ? d.getInteger("quarters") /4F * 90 : 90
            ) {
                @Override
                public boolean applyIfMissing() {
                    return true;
                }
            };
        }
    }

    private static class SmoothingMapper implements TagMapper<TrackSmoothing> {
        @Override
        public TagAccessor<TrackSmoothing> apply(Class<TrackSmoothing> type, String fieldName, TagField tag) {
            return new TagAccessor<TrackSmoothing>(
                    (d, o) -> d.setEnum(fieldName, o),
                    nbt -> {
                        if (nbt.hasKey(fieldName)) {
                            return nbt.getEnum(fieldName, type);
                        }
                        return nbt.getEnum("type", TrackItems.class) == TrackItems.SLOPE ?
                                TrackSmoothing.NEITHER : TrackSmoothing.BOTH;
                    }
            ) {
                @Override
                public boolean applyIfMissing() {
                    return true;
                }
            };
        }
    }

    public static class Builder {
        public Gauge gauge;
        public TrackItems type;
        public int length;
        public float degrees;
        public float curvosity;
        public TrackPositionType posType;
        public TrackSmoothing smoothing;
        public TrackDirection direction;
        public ItemStack railBed;
        public ItemStack railBedFill;
        public boolean isPreview;
        public boolean isGradeCrossing;
        public String track;

        private Builder(RailSettings settings) {
            this.gauge = settings.gauge;
            this.track = settings.track;
            this.type = settings.type;
            this.length = settings.length;
            this.degrees = settings.degrees;
            this.curvosity = settings.curvosity;
            this.posType = settings.posType;
            this.smoothing = settings.smoothing;
            this.direction = settings.direction;
            this.railBed = settings.railBed;
            this.railBedFill = settings.railBedFill;
            this.isPreview = settings.isPreview;
            this.isGradeCrossing = settings.isGradeCrossing;
        }

        public RailSettings build() {
            return new RailSettings(
                    gauge,
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
    }

    public RailSettings withLength(int length) {
       return new RailSettings(
				gauge,
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
       ) ;
    }

    public RailSettings withType(TrackItems type) {
        return new RailSettings(
                gauge,
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
    
    public RailSettings withTrack(String track) {
        return new RailSettings(
                gauge,
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

    public RailSettings withBed(ItemStack railBed) {
        return new RailSettings(
                gauge,
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

    public RailSettings withBedFill(ItemStack railBedFill) {
        return new RailSettings(
                gauge,
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


    public RailSettings withGauge(Gauge gauge) {
        return new RailSettings(
                gauge,
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
}
