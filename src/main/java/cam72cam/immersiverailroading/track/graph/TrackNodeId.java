package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.serialization.TagSerializer;

import java.util.Objects;

@TagMapped(TrackNodeId.Mapper.class)
public record TrackNodeId(
        long regionPos,
        Vec3i regionBlockPos,
        int pathIndex,
        Gauge gauge,
        int pointIndex
) {
    public TrackNodeId {
        Objects.requireNonNull(regionBlockPos, "regionBlockPos");
        Objects.requireNonNull(gauge, "gauge");
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        @TagField("regionPos")
        public long regionPos;
        @TagField("regionBlockPos")
        public Vec3i regionBlockPos;
        @TagField("pathIndex")
        public int pathIndex;
        @TagField("gauge")
        public Gauge gauge;
        @TagField("pointIndex")
        public int pointIndex;

        private Mutable(TrackNodeId id) {
            this.regionPos = id.regionPos();
            this.regionBlockPos = id.regionBlockPos();
            this.pathIndex = id.pathIndex();
            this.gauge = id.gauge();
            this.pointIndex = id.pointIndex();
        }

        public Mutable(TagCompound data) throws SerializationException {
            TagSerializer.deserialize(data, this);
        }

        public TrackNodeId immutable() {
            return new TrackNodeId(regionPos, regionBlockPos, pathIndex, gauge, pointIndex);
        }
    }

    public static class Mapper implements TagMapper<TrackNodeId> {
        @Override
        public TagAccessor<TrackNodeId> apply(Class<TrackNodeId> type, String fieldName, TagField tag)
                throws SerializationException {
            return new TagAccessor<>(
                    (nbt, o) -> {
                        if (o == null) {
                            nbt.remove(fieldName);
                            return;
                        }
                        TagCompound target = new TagCompound();
                        try {
                            TagSerializer.serialize(target, o.mutable());
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                        nbt.set(fieldName, target);
                    },
                    nbt -> {
                        if (!nbt.hasKey(fieldName)) {
                            return null;
                        }
                        try {
                            return new Mutable(nbt.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}