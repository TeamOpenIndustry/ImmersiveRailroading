package cam72cam.immersiverailroading.util;

import cam72cam.mod.serialization.*;

import java.util.function.Consumer;

@TagMapped(TrackSnapSettings.TagMapper.class)
public record TrackSnapSettings(
        boolean snapPos,
        boolean snapYaw,
        boolean snapPitch,
        boolean snapRoll){
    public TrackSnapSettings() {
        this(true, true, false, false);
    }

    public static class Mutable {
        @TagField("snapPos")
        public boolean snapPos;
        @TagField("snapYaw")
        public boolean snapYaw;
        @TagField("snapPitch")
        public boolean snapPitch;
        @TagField("snapRoll")
        public boolean snapRoll;

        public Mutable(TrackSnapSettings type) {
            this.snapPos = type.snapPos();
            this.snapYaw = type.snapYaw();
            this.snapPitch = type.snapPitch();
            this.snapRoll = type.snapRoll();
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            TrackSnapSettings type = new TrackSnapSettings();
            this.snapPos = type.snapPos();
            this.snapYaw = type.snapYaw();
            this.snapPitch = type.snapPitch();
            this.snapRoll = type.snapRoll();

            TagSerializer.deserialize(data, this);
        }

        public TrackSnapSettings immutable() {
            return new TrackSnapSettings(
                    snapPos,
                    snapYaw,
                    snapPitch,
                    snapRoll
            );
        }
    }

    public TrackSnapSettings.Mutable mutable() {
        return new TrackSnapSettings.Mutable(this);
    }

    public TrackSnapSettings with(Consumer<TrackSnapSettings.Mutable> mod) {
        TrackSnapSettings.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<TrackSnapSettings> {
        @Override
        public TagAccessor<TrackSnapSettings> apply(Class<TrackSnapSettings> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        TagCompound target = new TagCompound();
                        try {
                            TagSerializer.serialize(target, o.mutable());
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                        d.set(fieldName, target);
                    },
                    d -> {
                        if(!d.hasKey(fieldName)){
                            return null;
                        }
                        try {
                            return new TrackSnapSettings.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}
