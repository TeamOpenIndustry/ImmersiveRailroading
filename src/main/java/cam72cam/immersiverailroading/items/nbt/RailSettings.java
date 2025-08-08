package cam72cam.immersiverailroading.items.nbt;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.*;

import java.util.function.Consumer;

@TagMapped(RailSettings.Mapper.class)
public class RailSettings {
    public final Gauge gauge;
    public final TrackItems type;
    public final int length;
    public final float degrees;
    public final float curvosity;
    public final TrackPositionType posType;
    public final TrackSmoothing smoothing;
    public final TrackDirection direction;
    public final ItemStack railBed;
    public final ItemStack railBedFill;
    public final boolean isPreview;
    public final boolean isGradeCrossing;
    public final String track;
    public final int transfertableEntryCount;
    public final int transfertableEntrySpacing;

    public RailSettings(Gauge gauge, String track, TrackItems type, int length, float degrees, float curvosity, TrackPositionType posType, TrackSmoothing smoothing, TrackDirection direction, ItemStack railBed, ItemStack railBedFill, boolean isPreview, boolean isGradeCrossing, int count, int spacing) {
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
        this.transfertableEntryCount = count;
        this.transfertableEntrySpacing = spacing;
    }

    public void write(ItemStack stack) {
        TagCompound data = new TagCompound();
        try {
            TagSerializer.serialize(data, mutable());
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        stack.setTagCompound(data);
    }

    public static RailSettings from(ItemStack stack) {
        try {
            return new Mutable(stack.getTagCompound()).immutable();
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public RailSettings with(Consumer<Mutable> mod) {
        Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
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

    public static class Mutable {
        @TagField(value = "gauge")
        public Gauge gauge;
        @TagField("type")
        public TrackItems type;
        @TagField("length")
        public int length;
        @TagField(value = "degrees", mapper = DegreesMapper.class)
        public float degrees;
        @TagField("curvosity")
        public float curvosity;
        @TagField("pos_type")
        public TrackPositionType posType;
        @TagField(value = "smoothing", mapper = SmoothingMapper.class)
        public TrackSmoothing smoothing;
        @TagField("direction")
        public TrackDirection direction;
        @TagField("bedItem")
        public ItemStack railBed;
        @TagField("bedFill")
        public ItemStack railBedFill;
        @TagField("isPreview")
        public boolean isPreview;
        @TagField("isGradeCrossing")
        public boolean isGradeCrossing;
        @TagField("track")
        public String track;

        @TagField("transfertableEntryCount")
        public int transfertableEntryCount;
        @TagField("transfertableEntrySpacing")
        public int transfertableEntrySpacing;

        private Mutable(RailSettings settings) {
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
            this.transfertableEntryCount = settings.transfertableEntryCount;
            this.transfertableEntrySpacing = settings.transfertableEntrySpacing;
        }

        private Mutable(TagCompound data) throws SerializationException {
            // Defaults
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
            transfertableEntryCount = 1;
            transfertableEntrySpacing = 1;

            TagSerializer.deserialize(data, this);
        }

        public RailSettings immutable() {
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
                    isGradeCrossing,
                    transfertableEntryCount,
                    transfertableEntrySpacing
            );
        }
    }

    // This assumes that a null RailSettings is serialized.
    public static class Mapper implements TagMapper<RailSettings> {
        @Override
        public TagAccessor<RailSettings> apply(Class<RailSettings> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> {
                        TagCompound target = new TagCompound();
                        try {
                            TagSerializer.serialize(target, o.mutable());
                        } catch (SerializationException e) {
                            // This is messy
                            throw new RuntimeException(e);
                        }
                        d.set(fieldName, target);
                    },
                    d -> {
                        try {
                            return new Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}
