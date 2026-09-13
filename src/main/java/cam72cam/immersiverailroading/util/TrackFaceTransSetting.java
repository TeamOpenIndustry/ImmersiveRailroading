package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.TextUtil;

import java.util.Locale;
import java.util.function.Consumer;

@TagMapped(TrackFaceTransSetting.TagMapper.class)
public record TrackFaceTransSetting(
        double bedThickness,
        FacePivotType facePivotType,
        Vec3d facePivotOffset) {

    public TrackFaceTransSetting() {
        this(0.1f, FacePivotType.RAIL_FACE_CENTER, Vec3d.ZERO);
    }

    public enum FacePivotType {
        RAIL_FACE_CENTER,
        CURVE_CENTER,
        CUSTOM,
        ;

        @Override
        public String toString() {
            return TextUtil.translate("track.immersiverailroading:face_pivot_type." + name().toLowerCase(Locale.ROOT));
        }
    }

    public Vec3d getFacePivotOffset(double trackHeight) {
        Vec3d res;
        switch (facePivotType) {
            case RAIL_FACE_CENTER -> res = new Vec3d(0, trackHeight, 0);
            case CUSTOM -> res = facePivotOffset;
            default -> res = Vec3d.ZERO;
        }
        return res;
    }

    public static class Mutable {
        @TagField("bedThickness")
        public double bedThickness;
        @TagField("facePivotType")
        public FacePivotType facePivotType;
        @TagField("facePivotOffset")
        public Vec3d facePivotOffset;

        public Mutable(TrackFaceTransSetting setting) {
            this.bedThickness = setting.bedThickness;
            this.facePivotType = setting.facePivotType;
            this.facePivotOffset = setting.facePivotOffset;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            TrackFaceTransSetting setting = new TrackFaceTransSetting();
            this.bedThickness = setting.bedThickness;
            this.facePivotType = setting.facePivotType;
            this.facePivotOffset = setting.facePivotOffset;

            TagSerializer.deserialize(data, this);
        }

        public TrackFaceTransSetting immutable() {
            return new TrackFaceTransSetting(
                    bedThickness,
                    facePivotType,
                    facePivotOffset
            );
        }
    }

    public TrackFaceTransSetting.Mutable mutable() {
        return new TrackFaceTransSetting.Mutable(this);
    }

    public TrackFaceTransSetting with(Consumer<TrackFaceTransSetting.Mutable> mod) {
        TrackFaceTransSetting.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<TrackFaceTransSetting> {
        @Override
        public TagAccessor<TrackFaceTransSetting> apply(Class<TrackFaceTransSetting> type, String fieldName, TagField tag) {
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
                            return new TrackFaceTransSetting.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }
}
