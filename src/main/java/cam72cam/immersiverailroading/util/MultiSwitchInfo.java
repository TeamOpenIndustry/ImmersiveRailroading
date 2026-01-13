package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@TagMapped(MultiSwitchInfo.TagMapper.class)
public class MultiSwitchInfo {
    public final List<SingleWayInfo> wayList;
    public final TrackItems realShapeType;
    public final boolean isMultiSwitchWay;//this should only be overwrite to true in BuilderMultiSwitch
    public final int orderAsChild;

    public MultiSwitchInfo(List<SingleWayInfo> wayList, TrackItems realShapeType, int orderAsChild, boolean isMultiSwitchWay) {
        this.wayList = wayList;
        this.realShapeType =  realShapeType;
        this.orderAsChild = orderAsChild;
        this.isMultiSwitchWay = isMultiSwitchWay;
    }

    public static class Mutable {
        @TagField(value = "wayList",mapper = WayListMapper.class)
        public List<SingleWayInfo> wayList;
        @TagField("realShapeType")
        public TrackItems realShapeType;
        @TagField("orderAsChild")
        public int orderAsChild;

        @TagField("isMultiSwtchWay")
        public boolean isMultiSwtchWay;

        public Mutable(MultiSwitchInfo info) {
            this.wayList = info.wayList;
            this.realShapeType = info.realShapeType;
            this.orderAsChild = info.orderAsChild;
            this.isMultiSwtchWay = info.isMultiSwitchWay;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            realShapeType = TrackItems.STRAIGHT;
            wayList = new ArrayList<>();
            wayList.add(new SingleWayInfo(SingleWayInfo.defaultSettings,SingleWayInfo.defaultPos,null,0));
            orderAsChild = 0;//0=straight(parent)as default;1=MID1,2=MID2,3=MID3,4=MID4,5=TURN
            isMultiSwtchWay = false;

            TagSerializer.deserialize(data, this);
        }

        public MultiSwitchInfo immutable() {
            return new MultiSwitchInfo(
                    wayList,
                    realShapeType,
                    orderAsChild,
                    isMultiSwtchWay
            );
        }
    }

    public MultiSwitchInfo.Mutable mutable() {
        return new MultiSwitchInfo.Mutable(this);
    }
    public static MultiSwitchInfo from(ItemStack stack) {
        TagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey("multiSwitchInfo")) {
            //default fallback
            List<SingleWayInfo> wayList = new ArrayList<>();
            wayList.add(new SingleWayInfo(SingleWayInfo.defaultSettings,SingleWayInfo.defaultPos,null,0));
            return new MultiSwitchInfo(wayList, TrackItems.TURN, 0, false);
        }

        try {
            TagCompound multiSwitchData = root.get("multiSwitchInfo");
            return new MultiSwitchInfo.Mutable(multiSwitchData).immutable();
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }
    public void write(ItemStack stack) {
        TagCompound root = stack.getTagCompound();
        if (root == null) {
            root = new TagCompound();
        }

        TagCompound multiSwitchData = new TagCompound();
        try {
            TagSerializer.serialize(multiSwitchData, mutable());
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }

        root.set("multiSwitchInfo", multiSwitchData);

        stack.setTagCompound(root);
    }
    public MultiSwitchInfo with(Consumer<MultiSwitchInfo.Mutable> mod) {
        MultiSwitchInfo.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    public static MultiSwitchInfo writePlacement(MultiSwitchInfo multiSwitchInfo,PlacementInfo placementInfo) {
        if(multiSwitchInfo != null && multiSwitchInfo.wayList != null){
            for(int i=0; i<multiSwitchInfo.wayList.size(); i++) {
                SingleWayInfo singleWayInfo = multiSwitchInfo.wayList.get(i);
                SingleWayInfo finalSingleWayInfo = singleWayInfo;

                singleWayInfo = singleWayInfo.with(m -> m.placementInfo = placementInfo.withDirection(finalSingleWayInfo.settings.direction));
                multiSwitchInfo.wayList.set(i, singleWayInfo);
            }
        }
        return multiSwitchInfo;
    }
    private static class WayListMapper implements cam72cam.mod.serialization.TagMapper<List<SingleWayInfo>> {
        public TagAccessor<List<SingleWayInfo>> apply(Class<List<SingleWayInfo>> t, String fieldname, TagField tag) {
            return new TagAccessor<>(
                    (nbt, list) -> {
                        if(list == null){
                            nbt.remove(fieldname);
                            return;
                        }
                        TagCompound wayListTag = new TagCompound();
                        for (int i = 0; i < list.size(); i++) {
                            TagCompound singleWayInfo = new TagCompound();
                            try {
                                TagSerializer.serialize(singleWayInfo, new SingleWayInfo.Mutable(list.get(i)));
                            } catch (SerializationException e) {
                                throw new RuntimeException(e);
                            }
                            wayListTag.set(i + "", singleWayInfo);
                        }
                        wayListTag.setInteger("amount",list.size());
                        nbt.set(fieldname,wayListTag);
                    },
                    nbt -> {
                        if(!nbt.hasKey(fieldname)){
                            return null;
                        }
                        TagCompound wayListTag = nbt.get(fieldname);
                        int amount = wayListTag.getInteger("amount");
                        List<SingleWayInfo> list = new ArrayList<>();
                        for (int i = 0; i < amount; i++) {
                            try {
                                list.add(new SingleWayInfo.Mutable(wayListTag.get(i + "")).immutable());
                            } catch (SerializationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return list;
                    }
            );
        }
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<MultiSwitchInfo> {
        @Override
        public TagAccessor<MultiSwitchInfo> apply(Class<MultiSwitchInfo> type, String fieldName, TagField tag) {
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
                            return new MultiSwitchInfo.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}
