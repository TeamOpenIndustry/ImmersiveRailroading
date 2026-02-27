package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.TextUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@TagMapped(RollAndOffsetInfo.TagMapper.class)
public class RollAndOffsetInfo {//TODO: although it works now but List is not really cloned so there might be some potential safety problems
    public final RollYOffsetType offsetType;
    private final List<Double> ls;//it is l and x and the same time, l is for outer curve it effects, x is for curves this stores
    //Roll
    /**
     * This stores [Superelevation(UNIT: Centimeter) * Gauge Scale] instead of roll angle.
     * Based on Standard Gauge, if in gauge X mm, it will be scaled to rollMax * X / 1435 Centimeters.
     */
    private final List<Vec3d> rolls;
    private final List<Vec3d> rollCtrls;
    //Y Offset
    /**
     * This stores [Height Offset(UNIT: Meter) * Gauge Scale].
     * Based on Standard Gauge, if in Gauge X mm, it will be scaled to yOffset * X / 1435 Meters.
     */
    private final List<Vec3d> yOffsets;
    private final List<Vec3d> yOffsetCtrls;
    //Z Offset
    /**
     * This stores [Width Offset(UNIT: Meter) * Gauge Scale].
     * Based on Standard Gauge, if in Gauge X mm, it will be scaled to zOffset * X / 1435 Meters.
     */
    private final List<Vec3d> zOffsets;
    private final List<Vec3d> zOffsetCtrls;

    public RollAndOffsetInfo(
            RollYOffsetType offsetType,
            List<Double> ls, List<Vec3d> rolls, List<Vec3d> rollCtrls, List<Vec3d> yOffsets, List<Vec3d> yOffsetlCtrls, List<Vec3d> zOffsets, List<Vec3d> zOffsetCtrls
    ) {
        this.offsetType = offsetType;

        this.ls = ls;
        this.rolls = rolls;
        this.rollCtrls = rollCtrls;
        this.yOffsets = yOffsets;
        this.yOffsetCtrls = yOffsetlCtrls;
        this.zOffsets = zOffsets;
        this.zOffsetCtrls = zOffsetCtrls;
    }

    public static RollAndOffsetInfo getDefault() {
        return new RollAndOffsetInfo();
    }
    private RollAndOffsetInfo() {
        offsetType = RollYOffsetType.MID;

        ls = new ArrayList<>();
        rolls = new ArrayList<>();
        rollCtrls = new ArrayList<>();
        yOffsets = new ArrayList<>();
        yOffsetCtrls = new ArrayList<>();
        zOffsets = new ArrayList<>();
        zOffsetCtrls = new ArrayList<>();

        ls.add(0d); ls.add(1d);
        rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
        rollCtrls.add(new Vec3d(1d / 3, 0, 0)); rollCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
        yOffsets.add(new Vec3d(0, 0, 0)); yOffsets.add(new Vec3d(1, 0, 0));
        yOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); yOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
        zOffsets.add(new Vec3d(0, 0, 0)); zOffsets.add(new Vec3d(1, 0, 0));
        zOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); zOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
    }

    public static class Mutable {
        @TagField("offsetType")
        public RollYOffsetType offsetType;
        @TagField(value = "ls", mapper = DoubleListMapper.class)
        private List<Double> ls;
        @TagField(value = "rolls", mapper = Vec3dListMapper.class)
        private List<Vec3d> rolls;
        @TagField(value = "rollCtrls", mapper = Vec3dListMapper.class)
        private List<Vec3d> rollCtrls;
        @TagField(value = "yOffsets", mapper = Vec3dListMapper.class)
        private List<Vec3d> yOffsets;
        @TagField(value = "yOffsetCtrls", mapper = Vec3dListMapper.class)
        private List<Vec3d> yOffsetCtrls;
        @TagField(value = "zOffsets", mapper = Vec3dListMapper.class)
        private List<Vec3d> zOffsets;
        @TagField(value = "zOffsetCtrls", mapper = Vec3dListMapper.class)
        private List<Vec3d> zOffsetCtrls;

        public Mutable(RollAndOffsetInfo rollAndOffsetInfo) {
            this.offsetType = rollAndOffsetInfo.offsetType;

            this.ls = rollAndOffsetInfo.ls;
            this.rolls = rollAndOffsetInfo.rolls;
            this.rollCtrls = rollAndOffsetInfo.rollCtrls;
            this.yOffsets = rollAndOffsetInfo.yOffsets;
            this.yOffsetCtrls = rollAndOffsetInfo.yOffsetCtrls;
            this.zOffsets = rollAndOffsetInfo.zOffsets;
            this.zOffsetCtrls = rollAndOffsetInfo.zOffsetCtrls;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            offsetType = RollYOffsetType.MID;

            ls = new ArrayList<>();
            rolls = new ArrayList<>();
            rollCtrls = new ArrayList<>();
            yOffsets = new ArrayList<>();
            yOffsetCtrls = new ArrayList<>();
            zOffsets = new ArrayList<>();
            zOffsetCtrls = new ArrayList<>();

            ls.add(0d); ls.add(1d);
            rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
            rollCtrls.add(new Vec3d(1d / 3, 0, 0)); rollCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            yOffsets.add(new Vec3d(0, 0, 0)); yOffsets.add(new Vec3d(1, 0, 0));
            yOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); yOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            zOffsets.add(new Vec3d(0, 0, 0)); zOffsets.add(new Vec3d(1, 0, 0));
            zOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); zOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));

            TagSerializer.deserialize(data, this);
        }

        public RollAndOffsetInfo immutable() {
            return new RollAndOffsetInfo(
                    offsetType,

                    ls,
                    rolls,
                    rollCtrls,
                    yOffsets,
                    yOffsetCtrls,
                    zOffsets,
                    zOffsetCtrls
            );
        }

        //function for Gui config, notice that when using these, Lists CANT be NULL!
        /**
         * This will return -1 if L not in range
         */
        public int findPhysicalIndex(double l) {//return physical index
            int res = -1;
            for (int i = 0; i < ls.size(); i += 2) {
                if(ls.get(i) == l) {
                    res = i;
                    break;
                }
            }
            if(!ls.isEmpty() && ls.get(ls.size() - 1) == l) {
                res = ls.size()-1;
            }
            return res;
        }

        public boolean tryInsertBySubSplit(double l) {
            if(findPhysicalIndex(l) != -1) return false;

            List<Pair<Double, Double>> divider = new ArrayList<>();
            divider.add(Pair.of(0d, l));
            divider.add(Pair.of(l, 1d));

            RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(
                    offsetType,
                    ls, rolls, rollCtrls, yOffsets, yOffsetCtrls, zOffsets, zOffsetCtrls
            );
            List<RollAndOffsetInfo> res = rollAndOffsetInfo.subSplit(divider, false);//will divide it manually here, should be faster

            ls.clear();
            ls.addAll(res.get(0).ls);
            ls.addAll(res.get(1).ls);

            rolls.clear();
            rolls.addAll(res.get(0).rolls);
            rolls.addAll(res.get(1).rolls);

            rollCtrls.clear();
            rollCtrls.addAll(res.get(0).rollCtrls);
            rollCtrls.addAll(res.get(1).rollCtrls);

            yOffsets.clear();
            yOffsets.addAll(res.get(0).yOffsets);
            yOffsets.addAll(res.get(1).yOffsets);

            yOffsetCtrls.clear();
            yOffsetCtrls.addAll(res.get(0).yOffsetCtrls);
            yOffsetCtrls.addAll(res.get(1).yOffsetCtrls);

            zOffsets.clear();
            zOffsets.addAll(res.get(0).zOffsets);
            zOffsets.addAll(res.get(1).zOffsets);

            zOffsetCtrls.clear();
            zOffsetCtrls.addAll(res.get(0).zOffsetCtrls);
            zOffsetCtrls.addAll(res.get(1).zOffsetCtrls);

            return true;
        }

        public boolean tryDeleteDirectly(double l) {
            int idx = findPhysicalIndex(l);
            if(idx == -1 || idx == 0 || idx == ls.size() - 1) return false;

            ls.remove(idx - 1);
            ls.remove(idx - 1);

            rolls.remove(idx - 1);
            rolls.remove(idx - 1);

            rollCtrls.remove(idx - 1);
            rollCtrls.remove(idx - 1);

            yOffsets.remove(idx - 1);
            yOffsets.remove(idx - 1);

            yOffsetCtrls.remove(idx - 1);
            yOffsetCtrls.remove(idx - 1);

            zOffsets.remove(idx - 1);
            zOffsets.remove(idx - 1);

            zOffsetCtrls.remove(idx - 1);
            zOffsetCtrls.remove(idx - 1);

            return true;
        }

        public void resetAll() {
            ls.clear();
            rolls.clear();
            rollCtrls.clear();
            yOffsets.clear();
            yOffsetCtrls.clear();
            zOffsets.clear();
            zOffsetCtrls.clear();

            ls.add(0d); ls.add(1d);
            rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
            rollCtrls.add(new Vec3d(1d / 3, 0, 0)); rollCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            yOffsets.add(new Vec3d(0, 0, 0)); yOffsets.add(new Vec3d(1, 0, 0));
            yOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); yOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            zOffsets.add(new Vec3d(0, 0, 0)); zOffsets.add(new Vec3d(1, 0, 0));
            zOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); zOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
        }

        public boolean tryDeltaValue(double l, double val, ExtraInfoType type) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return false;

            List<Vec3d> points;
            List<Vec3d> ctrls;

            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return false;
            }

            Vec3d newValue = new Vec3d(points.get(idx).x, points.get(idx).y, val);
            Vec3d oldValue = ctrls.get(idx);
            double delta = val - points.get(idx).z;
            points.set(idx, newValue);
            ctrls.set(idx, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));

            if(idx > 0 && idx < ls.size() - 1) {
                oldValue = ctrls.get(idx - 1);
                points.set(idx - 1, newValue);
                ctrls.set(idx - 1, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));
            }

            return true;
        }
        public String getValueDisplay(double l, ExtraInfoType type) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return "null";

            List<Vec3d> points;

            switch (type) {
                case ROLL:
                    points = rolls;
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return "null";
            }

            return String.format("%.4f", points.get(idx).z);
        }

        public boolean trySetHandleXLen(double l, double val, ExtraInfoType type, boolean editLeft, double length) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return false;
            if(idx == 0 && editLeft) return false;
            if(idx == ls.size() - 1 && !editLeft) return false;
            if(val < 1e-1) return false;

            double newHandleXLen = val / length;
            List<Vec3d> points;
            List<Vec3d> ctrls;

            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return false;
            }

            boolean feedback = false;

            int editIdx;
            double segmentLen;
            if(editLeft) {
                if(idx == ls.size() - 1) {
                    editIdx = idx;
                    segmentLen = Math.abs(ls.get(editIdx) - ls.get(editIdx - 1));
                }else {
                    editIdx = idx - 1;
                    segmentLen = Math.abs(ls.get(editIdx) - ls.get(editIdx - 1));
                }
            }else {
                editIdx = idx;
                segmentLen = Math.abs(ls.get(editIdx) - ls.get(editIdx + 1));
            }

            if(newHandleXLen < segmentLen * 0.5) {
                Vec3d oldCtrlHandle = ctrls.get(editIdx).subtract(points.get(editIdx));
                double scale = newHandleXLen / Math.abs(oldCtrlHandle.x);
                Vec3d newCtrlHandle = oldCtrlHandle.scale(scale);
                ctrls.set(editIdx, points.get(editIdx).add(newCtrlHandle));
                feedback = true;
            }

            return feedback;
        }
        public String getHandleXDisplay(double l, ExtraInfoType type, boolean editLeft, double length) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return "null";
            if(idx == 0 && editLeft) return "null";
            if(idx == ls.size() - 1 && !editLeft) return "null";

            List<Vec3d> points;
            List<Vec3d> ctrls;

            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return "null";
            }

            int displayIdx;
            if(editLeft) {
                if(idx == ls.size() - 1) {
                    displayIdx = idx;
                }else {
                    displayIdx = idx - 1;
                }
            }else {
                displayIdx = idx;
            }

            double res = Math.abs(ctrls.get(displayIdx).x - points.get(displayIdx).x);
            return String.format("%.4f", res * length);
        }

        public boolean trySetSlope(double l, double val, ExtraInfoType type, double length) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return false;


            List<Vec3d> points;
            List<Vec3d> ctrls;
            double slopeScale;

            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    slopeScale = 0.1;//centimeter/meter * 0.1 => 1/1000 slope
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    slopeScale = 0.001;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    slopeScale = 0.001;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return false;
            }

            double fullZWithLength = val * slopeScale * length;

            Vec3d oldCtrl = ctrls.get(idx);
            double newValueBase = points.get(idx).z;
            double newValueDelta = (ctrls.get(idx).x - points.get(idx).x) * fullZWithLength;
            double newValue = newValueBase + newValueDelta;
            ctrls.set(idx, new Vec3d(oldCtrl.x, oldCtrl.y, newValue));

            if(idx > 0 && idx < ls.size() - 1) {
                oldCtrl = ctrls.get(idx - 1);
                newValueBase = points.get(idx - 1).z;
                newValueDelta = (ctrls.get(idx - 1).x - points.get(idx - 1).x) * fullZWithLength;
                newValue = newValueBase + newValueDelta;
                ctrls.set(idx - 1, new Vec3d(oldCtrl.x, oldCtrl.y, newValue));
            }

            return true;
        }
        public String getSlopeDisplay(double l, ExtraInfoType type, double length) {
            int idx = findPhysicalIndex(l);
            if(idx == -1) return "null";

            List<Vec3d> points;
            List<Vec3d> ctrls;
            int slopeScale;

            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    slopeScale = 10;//centimeter/meter * 0.1 => 1/1000 slope
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    slopeScale = 1000;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    slopeScale = 1000;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return "null";
            }

            double tan = (ctrls.get(idx).z - points.get(idx).z) / (ctrls.get(idx).x - points.get(idx).x);
            tan /= length;
            tan *= slopeScale;
            return String.format("%.4f", tan);
        }
        public List<CubicCurve> toCurves(ExtraInfoType type, boolean swapYZ) {
            List<Vec3d> points;
            List<Vec3d> ctrls;
            switch (type) {
                case ROLL:
                    points = rolls;
                    ctrls = rollCtrls;
                    break;
                case Y_OFFSET:
                    points = yOffsets;
                    ctrls = yOffsetCtrls;
                    break;
                case Z_OFFSET:
                    points = zOffsets;
                    ctrls = zOffsetCtrls;
                    break;
                default:
                    ImmersiveRailroading.warn("invalid ExtraInfoType:" + type);
                    return null;
            }

            List<CubicCurve> curves = new ArrayList<>();
            for(int i = 0; i < ls.size(); i+=2) {
                CubicCurve curve;
                if(swapYZ) {//for screen rendering
                    Vec3d p1 = new Vec3d(points.get(i).x, points.get(i).z, points.get(i).y);
                    Vec3d ctrl1 = new Vec3d(ctrls.get(i).x, ctrls.get(i).z, ctrls.get(i).y);
                    Vec3d ctrl2Opposite = new Vec3d(ctrls.get(i + 1).x, ctrls.get(i + 1).z, ctrls.get(i + 1).y);
                    Vec3d p2 = new Vec3d(points.get(i + 1).x, points.get(i + 1).z, points.get(i + 1).y);
                    Vec3d ctrl2 = p2.scale(2).subtract(ctrl2Opposite);
                    curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                }else {
                    curve = new CubicCurve(points.get(i), ctrls.get(i), points.get(i + 1).scale(2).subtract(ctrls.get(i + 1)), points.get(i + 1));
                }
                curves.add(curve);
            }
            return curves;
        }
    }

    public RollAndOffsetInfo.Mutable mutable() {
        return new RollAndOffsetInfo.Mutable(this);
    }

    public RollAndOffsetInfo with(Consumer<RollAndOffsetInfo.Mutable> mod) {
        RollAndOffsetInfo.Mutable mutable = mutable();
        mod.accept(mutable);
        return mutable.immutable();
    }

    public static class Vec3dListMapper implements cam72cam.mod.serialization.TagMapper<List<Vec3d>> {
        public TagAccessor<List<Vec3d>> apply(Class<List<Vec3d>> t, String fieldname, TagField tag) {
            return new TagAccessor<>(
                    (nbt, list) -> {
                        if(list == null){
                            nbt.remove(fieldname);
                            return;
                        }
                        TagCompound vec3dListTag = new TagCompound();
                        for (int i = 0; i < list.size(); i++) {
                            vec3dListTag.setVec3d(i + "", list.get(i));
                        }
                        vec3dListTag.setInteger("amount", list.size());
                        nbt.set(fieldname,vec3dListTag);
                    },
                    nbt -> {
                        if(!nbt.hasKey(fieldname)){
                            return null;
                        }
                        TagCompound vec3dListTag = nbt.get(fieldname);
                        int amount = vec3dListTag.getInteger("amount");
                        List<Vec3d> list = new ArrayList<>();
                        for (int i = 0; i < amount; i++) {
                            list.add(vec3dListTag.getVec3d(i + ""));
                        }
                        return list;
                    }
            );
        }
    }

    public static class DoubleListMapper implements cam72cam.mod.serialization.TagMapper<List<Double>> {
        public TagAccessor<List<Double>> apply(Class<List<Double>> t, String fieldname, TagField tag) {
            return new TagAccessor<>(
                    (nbt, list) -> {
                        if(list == null){
                            nbt.remove(fieldname);
                            return;
                        }
                        TagCompound doubleListTag = new TagCompound();
                        for (int i = 0; i < list.size(); i++) {
                            doubleListTag.setDouble(i + "", list.get(i));
                        }
                        doubleListTag.setInteger("amount", list.size());
                        nbt.set(fieldname,doubleListTag);
                    },
                    nbt -> {
                        if(!nbt.hasKey(fieldname)){
                            return null;
                        }
                        TagCompound doubleListTag = nbt.get(fieldname);
                        int amount = doubleListTag.getInteger("amount");
                        List<Double> list = new ArrayList<>();
                        for (int i = 0; i < amount; i++) {
                            list.add(doubleListTag.getDouble(i + ""));
                        }
                        return list;
                    }
            );
        }
    }

    static class TagMapper implements cam72cam.mod.serialization.TagMapper<RollAndOffsetInfo> {
        @Override
        public TagAccessor<RollAndOffsetInfo> apply(Class<RollAndOffsetInfo> type, String fieldName, TagField tag) {
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
                            return new RollAndOffsetInfo.Mutable(d.get(fieldName)).immutable();
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }
    }

    @Override
    public String toString() {
        String id = "rollAndOffsetInfo:{";
        id += this.offsetType;
        if(this.ls != null) {
            for(int i = 0; i < this.ls.size(); i++){
                id += this.ls.get(i);
                id += this.rolls.get(i);
                id += this.rollCtrls.get(i);
                id += this.yOffsets.get(i);
                id += this.yOffsetCtrls.get(i);
                id += this.zOffsets.get(i);
                id += this.zOffsetCtrls.get(i);
            }
        }
        id += "}";
        return id;
    }
    public enum RollYOffsetType {
        MID(0),
        HIGH(1),
        LOW(2);
        private final int order;
        private static final RollYOffsetType[] BY_ORDER = values();
        RollYOffsetType(int order){
            this.order = order;
        }
        public int getOrder() {
            return this.order;
        }
        public static RollYOffsetType byOrder(int order) {
            if (order < 0 || order >= BY_ORDER.length) {
                return MID;
            }
            for (RollYOffsetType type : BY_ORDER) {
                if (type.order == order) {
                    return type;
                }
            }
            return MID;
        }
        public static final int amount = values().length;
        @Override
        public String toString() {
            return TextUtil.translate("track.immersiverailroading:rollYOffsetType." + super.toString().toLowerCase(Locale.ROOT));
        }
    }

    public enum ExtraInfoType {
        ROLL,
        Y_OFFSET,
        Z_OFFSET;

        @Override
        public String toString() {
            return TextUtil.translate("track.immersiverailroading:ExtraInfoType." + super.toString().toLowerCase(Locale.ROOT));
        }
    }

    public static List<Pair<Double,Double>> toRange(List<CubicCurve> subCurves) {
        List<Pair<Double,Double>> res = new ArrayList<>();
        for(CubicCurve subCurve : subCurves) {
            res.add(Pair.of(subCurve.lStart, subCurve.lEnd));
        }
        return res;
    }

    //subSplit
    //分类讨论:按照tStart到tEnd区间包含的t的点个数讨论
    //0个:new内存一段线即可，但是tStart和tEnd两端都需要计算，再参数化到01存储->ok
    //非0个:tStart和tEnd两端也都需要计算再参数化到01存储，中间的t就只要参数化到01
    //boundary:
    //1:tStart overlay with a point
    //2:tEnd overlay with a point
    public List<RollAndOffsetInfo> subSplit(List<Pair<Double,Double>> subCurves, boolean normalize) {//只有分段发生才会进入此处,但是某些bug导致传入的subsplit范围不合法（start和end重合）会导致ls.size()==0
        List<RollAndOffsetInfo> results = new ArrayList<>();

        for (Pair<Double,Double> subCurve : subCurves) {
            double lStart = subCurve.getLeft();
            double lEnd = subCurve.getRight();

            List<Double> newT = new ArrayList<>();
            List<Vec3d> newRolls = new ArrayList<>();
            List<Vec3d> newRollCtrls = new ArrayList<>();
            List<Vec3d> newYOffsets = new ArrayList<>();
            List<Vec3d> newYOffsetCtrls = new ArrayList<>();
            List<Vec3d> newZOffsets = new ArrayList<>();
            List<Vec3d> newZOffsetCtrls = new ArrayList<>();

            int logicIdxStart = (findRight(ls,lStart) + 1) / 2;//这两个查找方法（最好二分？）等于时都满足
            int logicIdxEnd = (findLeft(ls,lEnd) + 1) / 2;//搜到的是点对应的x是对的,处理成逻辑上的index

            int count = logicIdxEnd - logicIdxStart + 1;
            if(count == 0) {//由于查找是包含等于的，所以这里没找到的话说明严格为0，其实也侧面说明分段数超过2了
                newT.add(0.0);
                newT.add(1.0);

                {//roll
                    Vec3d p1 = rolls.get(logicIdxEnd * 2);
                    Vec3d p2 = rolls.get(logicIdxStart * 2 - 1);
                    Vec3d ctrl1 = rollCtrls.get(logicIdxEnd * 2);
                    Vec3d ctrl2 = rolls.get(logicIdxStart * 2 - 1).scale(2).subtract(rollCtrls.get(logicIdxStart * 2 - 1));

                    CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                    CubicCurve startCurve = getLeftByX(lStart, curve);
                    CubicCurve endCurve = getLeftByX(lEnd, curve);

                    p1 = startCurve.p2;
                    p2 = endCurve.p2;
                    ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);
                    ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                    newRolls.add(p1);
                    newRolls.add(p2);
                    newRollCtrls.add(ctrl1);
                    newRollCtrls.add(ctrl2);
                }

                {//YOffset
                    Vec3d p1 = yOffsets.get(logicIdxEnd * 2);
                    Vec3d p2 = yOffsets.get(logicIdxStart * 2 - 1);
                    Vec3d ctrl1 = yOffsetCtrls.get(logicIdxEnd * 2);
                    Vec3d ctrl2 = yOffsets.get(logicIdxStart * 2 - 1).scale(2).subtract(yOffsetCtrls.get(logicIdxStart * 2 - 1));

                    CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                    CubicCurve startCurve = getLeftByX(lStart, curve);
                    CubicCurve endCurve = getLeftByX(lEnd, curve);

                    p1 = startCurve.p2;
                    p2 = endCurve.p2;
                    ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);
                    ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                    newYOffsets.add(p1);
                    newYOffsets.add(p2);
                    newYOffsetCtrls.add(ctrl1);
                    newYOffsetCtrls.add(ctrl2);
                }

                {//ZOffset
                    Vec3d p1 = zOffsets.get(logicIdxEnd * 2);
                    Vec3d p2 = zOffsets.get(logicIdxStart * 2 - 1);
                    Vec3d ctrl1 = zOffsetCtrls.get(logicIdxEnd * 2);
                    Vec3d ctrl2 = zOffsets.get(logicIdxStart * 2 - 1).scale(2).subtract(zOffsetCtrls.get(logicIdxStart * 2 - 1));

                    CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                    CubicCurve startCurve = getLeftByX(lStart, curve);
                    CubicCurve endCurve = getLeftByX(lEnd, curve);

                    p1 = startCurve.p2;
                    p2 = endCurve.p2;
                    ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);
                    ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                    newZOffsets.add(p1);
                    newZOffsets.add(p2);
                    newZOffsetCtrls.add(ctrl1);
                    newZOffsetCtrls.add(ctrl2);
                }
            }else {
                {//roll
                    //start
                    if(lStart != ls.get(Physic(logicIdxStart))) {
                        Vec3d p1 = rolls.get(Physic(logicIdxStart - 1));
                        Vec3d p2 = rolls.get(Physic(logicIdxStart));
                        Vec3d ctrl1 = rollCtrls.get(Physic(logicIdxStart - 1));
                        Vec3d ctrl2 = rolls.get(Physic(logicIdxStart)).scale(2).subtract(rollCtrls.get(Physic(logicIdxStart)));//规定ctrl在右边，所以ctrl2都要取反一下

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve startCurve = getLeftByX(lStart, curve.reverse()).reverse();
                        p1 = startCurve.p1;
                        ctrl1 = startCurve.ctrl1;
                        ctrl2 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);

                        newT.add(p1.x);
                        newT.add(p2.x);
                        newRolls.add(p1);
                        newRolls.add(p2);
                        newRollCtrls.add(ctrl1);
                        newRollCtrls.add(ctrl2);
                    }
                    //mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newT.add(ls.get(Physic(i)));
                        newT.add(ls.get(Physic(i + 1)));//当idxStart==idxEnd时不会进入循环，不会越界
                        newRolls.add(rolls.get(Physic(i)));
                        newRolls.add(rolls.get(Physic(i + 1)));
                        newRollCtrls.add(rollCtrls.get(Physic(i)));
                        newRollCtrls.add(rollCtrls.get(Physic(i + 1)));
                    }
                    //end
                    if(lEnd != ls.get(Physic(logicIdxEnd))) {
                        Vec3d p1 = rolls.get(Physic(logicIdxEnd));
                        Vec3d p2 = rolls.get(Physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = rollCtrls.get(Physic(logicIdxEnd));
                        Vec3d ctrl2 = rolls.get(Physic(logicIdxEnd + 1)).scale(2).subtract(rollCtrls.get(Physic(logicIdxEnd + 1)));

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve endCurve = getLeftByX(lEnd, curve);
                        ctrl1 = endCurve.ctrl1;
                        ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);
                        p2 = endCurve.p2;

                        newT.add(p1.x);
                        newT.add(p2.x);
                        newRolls.add(p1);
                        newRolls.add(p2);
                        newRollCtrls.add(ctrl1);
                        newRollCtrls.add(ctrl2);
                    }
                }

                {//yOffset
                    //start
                    if(lStart != ls.get(Physic(logicIdxStart))) {
                        Vec3d p1 = yOffsets.get(Physic(logicIdxStart - 1));
                        Vec3d p2 = yOffsets.get(Physic(logicIdxStart));
                        Vec3d ctrl1 = yOffsetCtrls.get(Physic(logicIdxStart - 1));
                        Vec3d ctrl2 = yOffsets.get(Physic(logicIdxStart)).scale(2).subtract(yOffsetCtrls.get(Physic(logicIdxStart)));//规定ctrl在右边，所以ctrl2都要取反一下

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve startCurve = getLeftByX(lStart, curve.reverse()).reverse();
                        p1 = startCurve.p1;
                        ctrl1 = startCurve.ctrl1;
                        ctrl2 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);

                        newYOffsets.add(p1);
                        newYOffsets.add(p2);
                        newYOffsetCtrls.add(ctrl1);
                        newYOffsetCtrls.add(ctrl2);
                    }
                    //mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newYOffsets.add(yOffsets.get(Physic(i)));
                        newYOffsets.add(yOffsets.get(Physic(i + 1)));
                        newYOffsetCtrls.add(yOffsetCtrls.get(Physic(i)));
                        newYOffsetCtrls.add(yOffsetCtrls.get(Physic(i + 1)));
                    }
                    //end
                    if(lEnd != ls.get(Physic(logicIdxEnd))) {
                        Vec3d p1 = yOffsets.get(Physic(logicIdxEnd));
                        Vec3d p2 = yOffsets.get(Physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = yOffsetCtrls.get(Physic(logicIdxEnd));
                        Vec3d ctrl2 = yOffsets.get(Physic(logicIdxEnd + 1)).scale(2).subtract(yOffsetCtrls.get(Physic(logicIdxEnd + 1)));

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve endCurve = getLeftByX(lEnd, curve);
                        ctrl1 = endCurve.ctrl1;
                        ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);
                        p2 = endCurve.p2;

                        newYOffsets.add(p1);
                        newYOffsets.add(p2);
                        newYOffsetCtrls.add(ctrl1);
                        newYOffsetCtrls.add(ctrl2);
                    }
                }

                {//zOffset
                    //start
                    if(lStart != ls.get(Physic(logicIdxStart))) {
                        Vec3d p1 = zOffsets.get(Physic(logicIdxStart - 1));
                        Vec3d p2 = zOffsets.get(Physic(logicIdxStart));
                        Vec3d ctrl1 = zOffsetCtrls.get(Physic(logicIdxStart - 1));
                        Vec3d ctrl2 = zOffsets.get(Physic(logicIdxStart)).scale(2).subtract(zOffsetCtrls.get(Physic(logicIdxStart)));//规定ctrl在右边，所以ctrl2都要取反一下

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve startCurve = getLeftByX(lStart, curve.reverse()).reverse();
                        p1 = startCurve.p1;
                        ctrl1 = startCurve.ctrl1;
                        ctrl2 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);

                        newZOffsets.add(p1);
                        newZOffsets.add(p2);
                        newZOffsetCtrls.add(ctrl1);
                        newZOffsetCtrls.add(ctrl2);
                    }
                    //mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newZOffsets.add(zOffsets.get(Physic(i)));
                        newZOffsets.add(zOffsets.get(Physic(i + 1)));
                        newZOffsetCtrls.add(zOffsetCtrls.get(Physic(i)));
                        newZOffsetCtrls.add(zOffsetCtrls.get(Physic(i + 1)));
                    }
                    //end
                    if(lEnd != ls.get(Physic(logicIdxEnd))) {
                        Vec3d p1 = zOffsets.get(Physic(logicIdxEnd));
                        Vec3d p2 = zOffsets.get(Physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = zOffsetCtrls.get(Physic(logicIdxEnd));
                        Vec3d ctrl2 = zOffsets.get(Physic(logicIdxEnd + 1)).scale(2).subtract(zOffsetCtrls.get(Physic(logicIdxEnd + 1)));

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve endCurve = getLeftByX(lEnd, curve);
                        ctrl1 = endCurve.ctrl1;
                        ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);
                        p2 = endCurve.p2;

                        newZOffsets.add(p1);
                        newZOffsets.add(p2);
                        newZOffsetCtrls.add(ctrl1);
                        newZOffsetCtrls.add(ctrl2);
                    }
                }
            }

            if(normalize) results.add(normalize(
                    offsetType,
                    newT, newRolls, newRollCtrls, newYOffsets, newYOffsetCtrls, newZOffsets, newZOffsetCtrls,
                    lStart, lEnd
            ));

            else results.add(new RollAndOffsetInfo(
                    offsetType,
                    newT, newRolls, newRollCtrls, newYOffsets, newYOffsetCtrls, newZOffsets, newZOffsetCtrls
            ));
        }
        return results;
    }

    private int Physic(int logic) {
        if(logic == 0) return 0;
        if(logic * 2 > ls.size() - 1) return ls.size() - 1;
        return logic * 2;
    }

    public static CubicCurve getLeftByX(double x, CubicCurve curve) {//x is X-axis value not Length value!
        double localT = getTByX(x, curve);
        CubicCurve truncated = curve.getLeft(localT);
        double scale = (x - curve.p1.x) / (truncated.p2.x - curve.p1.x);//scale should be very close to 1 or plus 1

        Vec3d newCtrl1 = new Vec3d(
                curve.p1.x + (truncated.ctrl1.x - truncated.p1.x) * scale,
                truncated.ctrl1.y,
                truncated.ctrl1.z
        );

        Vec3d newCtrl2 = new Vec3d(
                x - (truncated.p2.x - truncated.ctrl2.x) * scale,
                truncated.ctrl2.y,
                truncated.ctrl2.z
        );

        Vec3d newP2 = new Vec3d(x, truncated.p2.y, truncated.p2.z);

        return new CubicCurve(curve.p1, newCtrl1, newCtrl2, newP2);
    }

    public static double getTByX(double targetX, CubicCurve curve) {//targetX is X-axis value not Length value!
        if (Math.abs(curve.p2.x - curve.p1.x) < 1e-12) return 0.5;
        double targetLocal = (targetX - curve.p1.x) / (curve.p2.x - curve.p1.x);
        double t = targetLocal;

        for (int i = 0; i < 10; i++) {
            Vec3d pos = curve.position(t);
            double error = pos.x - targetX;
            if (Math.abs(error) < 1e-12) break;

            Vec3d deriv = curve.derivative(t);
            if (Math.abs(deriv.x) < 1e-12) break;

            t = t - error / deriv.x;
            t = Math.max(0, Math.min(1, t));
        }

        return t;
    }

    private static RollAndOffsetInfo normalize(
            RollYOffsetType offsetType,
            List<Double> newT, List<Vec3d> newRolls, List<Vec3d> newRollCtrls, List<Vec3d> newYOffsets, List<Vec3d> newYOffsetCtrls, List<Vec3d> newZOffsets, List<Vec3d> newZOffsetCtrls,
            double tStart, double tEnd
    ) {

        for(int i = 0; i < newT.size(); i++) {
            //t
            double oldT = newT.get(i);
            double newTi = (oldT - tStart) / (tEnd - tStart);
            if (i == 0) newTi = 0.0;
            if (i == newT.size() - 1) newTi = 1.0;
            newT.set(i, newTi);

            //rolls
            Vec3d oldRoll = newRolls.get(i);
            newRolls.set(i, new Vec3d(newTi, oldRoll.y, oldRoll.z));
            Vec3d oldRollCtrl = newRollCtrls.get(i);
            double newRollCtrlX = (oldRollCtrl.x - tStart) / (tEnd - tStart);
            newRollCtrls.set(i, new Vec3d(newRollCtrlX, oldRollCtrl.y, oldRollCtrl.z));

            //yOffsets
            Vec3d oldYOffset = newYOffsets.get(i);
            newYOffsets.set(i, new Vec3d(newTi, oldYOffset.y, oldYOffset.z));
            Vec3d oldYOffsetCtrl = newYOffsetCtrls.get(i);
            double newYOffsetCtrlX = (oldYOffsetCtrl.x - tStart) / (tEnd - tStart);
            newYOffsetCtrls.set(i, new Vec3d(newYOffsetCtrlX, oldYOffsetCtrl.y, oldYOffsetCtrl.z));

            //zOffsets
            Vec3d oldZOffset = newZOffsets.get(i);
            newZOffsets.set(i, new Vec3d(newTi, oldZOffset.y, oldZOffset.z));
            Vec3d oldZOffsetCtrl = newZOffsetCtrls.get(i);
            double newZOffsetCtrlX = (oldZOffsetCtrl.x - tStart) / (tEnd - tStart);
            newZOffsetCtrls.set(i, new Vec3d(newZOffsetCtrlX, oldZOffsetCtrl.y, oldZOffsetCtrl.z));
        }
        return new RollAndOffsetInfo(
                offsetType,
                newT, newRolls, newRollCtrls, newYOffsets, newYOffsetCtrls, newZOffsets, newZOffsetCtrls
        );
    }

    private int findRight(List<Double> list, double target) {
        int left = 0, right = list.size() - 1;
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (list.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }

    private int findLeft(List<Double> list, double target) {
        int left = 0, right = list.size() - 1;
        while (left < right) {
            int mid = (left + right + 1) >>> 1;
            if (list.get(mid) > target) {
                right = mid - 1;
            } else {
                left = mid;
            }
        }
        return left;
    }

    public double getRoll(double l) {
        return getValue(this.ls, l, rolls, rollCtrls);
    }

    public double getYOffset(double l) {
        return getValue(this.ls, l, yOffsets, yOffsetCtrls);
    }

    public double getZOffset(double l) {
        return getValue(this.ls, l, zOffsets, zOffsetCtrls);
    }

    public static double getValue(List<Double>ls, double targetX, List<Vec3d> points, List<Vec3d> ctrls) {
        // notice that segmentIdx is not point index!
        int segmentIdx = findValidSegment(targetX, ls);

        int p1Idx = segmentIdx * 2;
        int p2Idx = p1Idx + 1;

        Vec3d p1 = points.get(p1Idx);
        Vec3d p2 = points.get(p2Idx);
        Vec3d ctrl1 = ctrls.get(p1Idx);
        Vec3d ctrl2Forward = ctrls.get(p2Idx);
        Vec3d ctrl2 = p2.scale(2).subtract(ctrl2Forward);

        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
        double localT = getTByX(targetX, curve);

        Vec3d pos = curve.position(localT);

        return pos.z;
    }

    public static int findValidSegment(double targetL, List<Double> ls) {
        for (int i = 0; i < ls.size() / 2; i++) {
            double segStart = ls.get(i * 2);
            double segEnd = ls.get(i * 2 + 1);
            if (targetL >= segStart && targetL <= segEnd) {
                return i;
            }
        }

        if (targetL <= ls.get(0)) return 0;
        return ls.size() / 2 - 1;
    }
}
