package cam72cam.immersiverailroading.track.graph;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.serialization.TagSerializer;

import java.util.HashSet;
import java.util.Set;

@TagMapped(TrackEdge.Mapper.class)
public class TrackEdge {
    public final TrackNodeId from;
    public final TrackNodeId to;
    public final EdgeType edgeType;
    public final Set<Long> coveringRegions = new HashSet<>();// not really final current

    public TrackEdge(TrackNodeId from, TrackNodeId to, EdgeType edgeType) {
        this.from = from;
        this.to = to;
        this.edgeType = edgeType;
    }

    public enum EdgeType {
        NORMAL,
        GAP
    }

    @Override
    public int hashCode() {
        int h = from.hashCode();
        h = 31 * h + to.hashCode();
        h = 31 * h + edgeType.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TrackEdge)) return false;
        TrackEdge o = (TrackEdge) obj;
        return from.equals(o.from) && to.equals(o.to) && edgeType == o.edgeType;
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        @TagField("from")
        public TrackNodeId from;
        @TagField("to")
        public TrackNodeId to;
        @TagField("edgeType")
        public EdgeType edgeType;
        @TagField(value = "coveringRegions", mapper = LongSetMapper.class)
        public Set<Long> coveringRegions;

        private Mutable(TrackEdge edge) {
            this.from = edge.from;
            this.to = edge.to;
            this.edgeType = edge.edgeType;
            this.coveringRegions = new HashSet<>(edge.coveringRegions);
        }

        public Mutable(TagCompound data) throws SerializationException {
            TagSerializer.deserialize(data, this);
        }

        public TrackEdge immutable() {
            TrackEdge edge = new TrackEdge(from, to, edgeType);
            edge.coveringRegions.addAll(coveringRegions);
            return edge;
        }
    }

    public static class Mapper implements TagMapper<TrackEdge> {
        @Override
        public TagAccessor<TrackEdge> apply(Class<TrackEdge> type, String fieldName, TagField tag)
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

    public static class LongSetMapper implements TagMapper<Set<Long>> {
        @Override
        public TagAccessor<Set<Long>> apply(Class<Set<Long>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (nbt, set) -> {
                        if (set == null) {
                            nbt.remove(fieldName);
                            return;
                        }
                        TagCompound setTag = new TagCompound();
                        int i = 0;
                        for (Long v : set) {
                            setTag.setLong(String.valueOf(i), v);
                            i++;
                        }
                        setTag.setInteger("amount", set.size());
                        nbt.set(fieldName, setTag);
                    },
                    nbt -> {
                        if (!nbt.hasKey(fieldName)) {
                            return null;
                        }
                        TagCompound setTag = nbt.get(fieldName);
                        int amount = setTag.getInteger("amount");
                        Set<Long> set = new HashSet<>(Math.max(16, amount * 2));
                        for (int i = 0; i < amount; i++) {
                            set.add(setTag.getLong(String.valueOf(i)));
                        }
                        return set;
                    }
            );
        }
    }
}