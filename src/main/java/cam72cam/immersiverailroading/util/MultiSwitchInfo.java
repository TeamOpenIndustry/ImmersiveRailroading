package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.mod.serialization.*;

import java.util.ArrayList;
import java.util.List;

@TagMapped(MultiSwitchInfo.TagMapper.class)
public class MultiSwitchInfo {

    public List<SingleWayInfo> wayList;
    public TrackItems typeAsChild;
    private int wayCount;

    public MultiSwitchInfo(List<SingleWayInfo> wayList, TrackItems typeAsChild) {
        this.wayList = wayList;
        this.wayCount = wayList.size();
        this.typeAsChild = typeAsChild;
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<MultiSwitchInfo> {
        @Override
        public TagAccessor<MultiSwitchInfo> apply(Class<MultiSwitchInfo> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (w, o) -> {
                        if (o == null) {
                            w.remove(fieldName);
                            return;
                        }

                        TagCompound info = new TagCompound();
                        TagCompound wayListTag = new TagCompound();
                        for (int i = 0; i < o.wayList.size(); i++) {
                            TagCompound singleWayInfo = new TagCompound();
                            TagSerializer.serialize(singleWayInfo, new SingleWayInfo.Mutable(o.wayList.get(i)));
                            wayListTag.set(i + "", singleWayInfo);
                        }

                        info.setEnum("typeAsChild", o.typeAsChild);
                        info.setInteger("wayCount", o.wayCount);
                        info.set("wayList", wayListTag);

                        w.set(fieldName, info);
                    },
                    (d, w) -> {
                        if (!d.hasKey(fieldName)) {
                            return null;
                        }
                        TagCompound info = d.get(fieldName);

                        TrackItems typeAsChild = info.getEnum("typeAsChild",TrackItems.class);
                        int count = info.getInteger("wayCount");
                        TagCompound wayListTag = info.get("wayList");

                        List<SingleWayInfo> wayList = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            wayList.add(new SingleWayInfo.Mutable(wayListTag.get(i + "")).immutable());
                        }
                        return new MultiSwitchInfo(wayList,typeAsChild);
                    }
            );
        }
    }
}
