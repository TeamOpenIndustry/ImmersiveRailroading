package cam72cam.immersiverailroading.util;

import cam72cam.mod.serialization.*;

import java.util.function.Consumer;

@TagMapped(EndPointData.TagMapper.class)
public record EndPointData (
        float radius,
        float heightOffset,
        float pitch,
        boolean pitchDegreeMode,
        boolean projectHandle) {
    public EndPointData(float radius, float heightOffset, float pitch, boolean pitchDegreeMode, boolean projectHandle) {
        this.radius = radius;
        this.heightOffset = heightOffset;
        this.pitch = pitch;
        this.pitchDegreeMode = pitchDegreeMode;
        this.projectHandle = projectHandle;
    }

    public EndPointData(float radius) {
        this(radius, 0f, 0f, false, true);
    }

    public static class Mutable {
        @TagField("radius")
        public float radius;
        @TagField("heightOffset")
        public float heightOffset;
        @TagField("pitch")
        public float pitch;
        @TagField("pitchDegreeMode")
        public boolean pitchDegreeMode;
        @TagField("projectHandle")
        public boolean projectHandle;

        public Mutable(EndPointData endPointData) {
            this.radius = endPointData.radius;
            this.heightOffset = endPointData.heightOffset;
            this.pitch = endPointData.pitch;
            this.pitchDegreeMode = endPointData.pitchDegreeMode;
            this.projectHandle = endPointData.projectHandle;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            EndPointData endPointData = new EndPointData(10);
            this.radius = endPointData.radius;
            this.heightOffset = endPointData.heightOffset;
            this.pitch = endPointData.pitch;
            this.pitchDegreeMode = endPointData.pitchDegreeMode;
            this.projectHandle = endPointData.projectHandle;

            TagSerializer.deserialize(data, this);
        }

        public EndPointData immutable() {
            return new EndPointData(
                    radius,
                    heightOffset,
                    pitch,
                    pitchDegreeMode,
                    projectHandle
            );
        }
    }

    public double getPitchRad() {
        return pitchDegreeMode ? Math.toRadians(pitch) : Math.atan2(pitch, 1000);
    }

    public String getPitchSetting() {
        return "" + pitchDegreeMode + " " + projectHandle;
    }

    public EndPointData.Mutable mutable() {
        return new EndPointData.Mutable(this);
    }

    public EndPointData with(Consumer<EndPointData.Mutable> mod) {
        EndPointData.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<EndPointData> {
        @Override
        public TagAccessor<EndPointData> apply(Class<EndPointData> type, String fieldName, TagField tag) {
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
                            return new EndPointData.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}
