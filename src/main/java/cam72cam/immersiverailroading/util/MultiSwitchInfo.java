package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.serialization.TagSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@TagMapped(MultiSwitchInfo.Mapper.class)
public record MultiSwitchInfo(
        List<MultiSwitchBranch> multiSwitchBranches,
        TrackItems realShapeType
) {
    public void write(ItemStack stack) {
        TagCompound data = stack.getTagCompound();
        try {
            TagSerializer.serialize(data, mutable());
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        stack.setTagCompound(data);
    }

    public static MultiSwitchInfo from(ItemStack stack) {
        try {
            return new MultiSwitchInfo.Mutable(stack.getTagCompound()).immutable();
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    public MultiSwitchInfo with(Consumer<MultiSwitchInfo.Mutable> mod) {
        MultiSwitchInfo.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    public MultiSwitchInfo writeBranchWith(Consumer<MultiSwitchBranch.Mutable> mod, int index) {
        if(index < 0 || index >= multiSwitchBranches.size()) return this;
        MultiSwitchBranch branch = multiSwitchBranches.get(index);
        branch = branch.with(mod);
        multiSwitchBranches.set(index, branch);
        return this;
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        @TagField(value = "multiSwitchBranches",mapper = MultiSwitchBranch.ListMapper.class)
        public List<MultiSwitchBranch> multiSwitchBranches;
        @TagField("realShapeType")
        public TrackItems realShapeType;

        private Mutable(MultiSwitchInfo info) {
            this.multiSwitchBranches = new ArrayList<>(info.multiSwitchBranches());
            this.realShapeType = info.realShapeType();
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            RailSettings settings0 = new RailSettings(
                    Gauge.standard(),
                    "default",
                    TrackItems.TURN_V2, TrackItems.TURN_V2,
                    10,
                    90f,
                    1,
                    TrackSmoothing.NEITHER_V2,
                    new EndPointData(0), new EndPointData(10),
                    RollAndOffsetInfo.getDefault(), RollAndOffsetInfo.getDefault(),
                    TrackDirection.LEFT,
                    new TrackFaceTransSetting(),
                    cam72cam.mod.item.ItemStack.EMPTY, cam72cam.mod.item.ItemStack.EMPTY,
                    false,
                    false,
                    1, 1
            );
            RailSettings settings1 = new RailSettings(
                    Gauge.standard(),
                    "default",
                    TrackItems.TURN_V2, TrackItems.TURN_V2,
                    20,
                    90f,
                    1,
                    TrackSmoothing.NEITHER_V2,
                    new EndPointData(0), new EndPointData(10),
                    RollAndOffsetInfo.getDefault(), RollAndOffsetInfo.getDefault(),
                    TrackDirection.LEFT,
                    new TrackFaceTransSetting(),
                    cam72cam.mod.item.ItemStack.EMPTY, cam72cam.mod.item.ItemStack.EMPTY,
                    false,
                    false,
                    1, 1
            );
            multiSwitchBranches = new ArrayList<>();
            multiSwitchBranches.add(new MultiSwitchBranch(settings0, null, null));
            multiSwitchBranches.add(new MultiSwitchBranch(settings1, null, null));
            realShapeType = TrackItems.STRAIGHT;
            TagSerializer.deserialize(data, this);
        }

        public MultiSwitchInfo immutable() {
            return new MultiSwitchInfo(
                    multiSwitchBranches,
                    realShapeType
            );
        }
    }

    public static class Mapper implements TagMapper<MultiSwitchInfo> {
        @Override
        public TagAccessor<MultiSwitchInfo> apply(Class<MultiSwitchInfo> type, String fieldName, TagField tag)
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