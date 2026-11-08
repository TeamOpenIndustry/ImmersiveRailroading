package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.serialization.TagSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TagMapped(MultiSwitchBranch.Mapper.class)
public record MultiSwitchBranch(
        RailSettings settings,
        PlacementInfo customInfo
) {
    public Mutable mutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        @TagField("settings")
        public RailSettings settings;
        @TagField("customInfo")
        public PlacementInfo customInfo;

        private Mutable(MultiSwitchBranch branch) {
            this.settings = branch.settings();
            this.customInfo = branch.customInfo();
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            TagSerializer.deserialize(data, this);
        }

        public MultiSwitchBranch immutable() {
            return new MultiSwitchBranch(settings, customInfo);
        }
    }

    public static class Mapper implements TagMapper<MultiSwitchBranch> {
        @Override
        public TagAccessor<MultiSwitchBranch> apply(Class<MultiSwitchBranch> type, String fieldName, TagField tag)
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

    public static class ListMapper implements TagMapper<List<MultiSwitchBranch>> {
        @Override
        public TagAccessor<List<MultiSwitchBranch>> apply(Class<List<MultiSwitchBranch>> type, String fieldName, TagField tag)
                throws SerializationException {
            return new TagAccessor<>(
                    (nbt, list) -> {
                        if (list == null) {
                            nbt.remove(fieldName);
                            return;
                        }
                        TagCompound listTag = new TagCompound();
                        for (int i = 0; i < list.size(); i++) {
                            TagCompound elemTag = new TagCompound();
                            try {
                                TagSerializer.serialize(elemTag, list.get(i).mutable());
                            } catch (SerializationException e) {
                                throw new RuntimeException(e);
                            }
                            listTag.set(String.valueOf(i), elemTag);
                        }
                        listTag.setInteger("amount", list.size());
                        nbt.set(fieldName, listTag);
                    },
                    nbt -> {
                        if (!nbt.hasKey(fieldName)) {
                            return null;
                        }
                        TagCompound listTag = nbt.get(fieldName);
                        int amount = listTag.getInteger("amount");
                        List<MultiSwitchBranch> list = new ArrayList<>(amount);
                        for (int i = 0; i < amount; i++) {
                            try {
                                list.add(new Mutable(listTag.get(String.valueOf(i))).immutable());
                            } catch (SerializationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return list;
                    }
            );
        }
    }

    @Override
    public String toString() {
        Object[] props = new Object [] {
                this.settings.type,
                this.settings.pickType,
                this.settings.length,
                this.settings.degrees,
                this.settings.curvosity,
                this.settings.nearPointData,
                this.settings.farPointData,
                this.settings.trackFaceTransSetting,
                this.settings.railBed,
                this.settings.gauge,
                this.settings.track,
                this.settings.smoothing,
                this.settings.isGradeCrossing,
        };
        String id = Arrays.toString(props);
        if (customInfo != null) {
            id += customInfo;
        }
        if(this.settings.rollAndOffsetInfo != null){
            id += this.settings.rollAndOffsetInfo;
        }
        if(this.settings.pickRollAndOffsetInfo != null){
            id += this.settings.pickRollAndOffsetInfo;
        }
        return id;
    }
}