package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.mod.serialization.*;

@TagMapped(SingleWayInfo.TagMapper.class)
public class SingleWayInfo {
    public final RailSettings settings;
    public final PlacementInfo placementInfo;
    public final PlacementInfo customInfo;
    public final int wayOrder;

    public SingleWayInfo(RailSettings settings,PlacementInfo placementInfo, PlacementInfo customInfo,int wayOrder) {
        this.settings = settings;
        this.placementInfo = placementInfo;
        this.customInfo = customInfo;
        this.wayOrder = wayOrder;
    }

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
}
