package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.TextUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

//TODO: Cleanup and docs
@TagMapped(RollAndOffsetInfo.TagMapper.class)
public record RollAndOffsetInfo(
        // Determines how roll compensation is applied to the generated track.
        // LEFT  : Keep the left rail at a constant world height.
        // RIGHT : Keep the right rail at a constant world height.
        // MID   : Rotate around the track centerline.
        RollAndVertOffsetAlignType rollOffsetType,

        // Whether roll should affect generated track blocks.
        // If false, only rendered rail models are rolled.
        boolean rollEffectTile,

        // Whether generated track tiles should also tilt with the track surface.
        boolean railBlockNormal,

        // Roll storage mode.
        // false = Superelevation (rail height difference, centimeter)
        // true  = Roll angle (degree)
        boolean degreeMode,

        // Vertical offset mode.
        // false = Offset along world Y axis.
        // true  = Offset along the local track normal.
        boolean offsetVertByNormal,

        // Curve parameterization
        //
        // Stores the normalized arc-length factor (0~1) of every control point.
        //
        // Note that this list is NOT a unique control point list.
        // Since each Bézier segment stores both its start and end control point,
        // adjacent segments intentionally duplicate their shared endpoint.
        //
        // Example:
        //
        //   Segment 0: P0 ----- P1
        //   Segment 1:          P1 ----- P2
        //
        // arcLenFactors:
        //   [P0, P1, P1, P2]
        //
        // This duplicated representation matches the storage layout of
        // rolls / yOffsets / zOffsets and their control handles, making each
        // Bézier segment completely self-contained.
        //
        // Only C1 continuity is enforced between adjacent segments.
        // The duplicated endpoint allows the two neighboring handle lengths
        // to be edited independently while constraining their tangents to remain collinear.
        List<Double> arcLenFactors,

        //Roll
        // Superelevation Mode:
        //     value = rail height difference (cm) scaled by gauge.
        // Degree Mode:
        //     value = roll angle in degrees.
        // This stores [Superelevation(UNIT: Centimeter) * Gauge Scale] instead of roll angle.
        // Based on Standard Gauge, if in gauge X (mm), it will be scaled to rollMax * X / Gauge.STANDARD.
        List<Vec3d> rolls,
        List<Vec3d> rollCtrls,

        // Y Offset
        // This stores [Height Offset(UNIT: Meter) * Gauge Scale].
        // Based on Standard Gauge, if in Gauge X (mm), it will be scaled to yOffset * X / Gauge.STANDARD.
        List<Vec3d> yOffsets,
        List<Vec3d> yOffsetCtrls,

        // Z Offset
        // This stores [Width Offset(UNIT: Meter) * Gauge Scale].
        // Based on Standard Gauge, if in Gauge X (mm), it will be scaled to zOffset * X / Gauge.STANDARD.
        List<Vec3d> zOffsets,
        List<Vec3d> zOffsetCtrls) {
    public RollAndOffsetInfo(RollAndVertOffsetAlignType rollOffsetType, boolean rollEffectTile, boolean railBlockNormal, boolean degreeMode, boolean offsetVertByNormal,
                             List<Double> arcLenFactors, List<Vec3d> rolls, List<Vec3d> rollCtrls,
                             List<Vec3d> yOffsets, List<Vec3d> yOffsetCtrls, List<Vec3d> zOffsets, List<Vec3d> zOffsetCtrls) {
        this.rollOffsetType = rollOffsetType;
        this.rollEffectTile = rollEffectTile;
        this.railBlockNormal = railBlockNormal;
        this.degreeMode = degreeMode;
        this.offsetVertByNormal = offsetVertByNormal;
        //Create unmodifiable view
        this.arcLenFactors = List.copyOf(arcLenFactors);
        this.rolls = List.copyOf(rolls);
        this.rollCtrls = List.copyOf(rollCtrls);
        this.yOffsets = List.copyOf(yOffsets);
        this.yOffsetCtrls = List.copyOf(yOffsetCtrls);
        this.zOffsets = List.copyOf(zOffsets);
        this.zOffsetCtrls = List.copyOf(zOffsetCtrls);
    }

    private RollAndOffsetInfo() {
        var arcLenFactors = List.of(0d, 1d);
        var rolls = List.of(new Vec3d(0, 0, 0), new Vec3d(1, 0, 0));
        var rollCtrls = List.of(new Vec3d(1d / 3, 0, 0), new Vec3d(1 + 1d / 3, 0, 0));
        var yOffsets = List.of(new Vec3d(0, 0, 0), new Vec3d(1, 0, 0));
        var yOffsetCtrls = List.of(new Vec3d(1d / 3, 0, 0), new Vec3d(1 + 1d / 3, 0, 0));
        var zOffsets = List.of(new Vec3d(0, 0, 0), new Vec3d(1, 0, 0));
        var zOffsetCtrls = List.of(new Vec3d(1d / 3, 0, 0), new Vec3d(1 + 1d / 3, 0, 0));

        this(RollAndVertOffsetAlignType.MID, true, true, false, false,
            arcLenFactors, rolls, rollCtrls, yOffsets, yOffsetCtrls, zOffsets, zOffsetCtrls);
    }

    public static RollAndOffsetInfo getDefault() {
        return new RollAndOffsetInfo();
    }

    public static class Mutable {
        @TagField("rollOffsetType")
        public RollAndVertOffsetAlignType rollOffsetType;
        @TagField("rollEffectTile")
        public boolean rollEffectTile;
        @TagField("railBlockNormal")
        public boolean railBlockNormal;
        @TagField("offsetVertByNormal")
        public boolean offsetVertByNormal;

        @TagField("degreeMode")
        public boolean degreeMode;
        @TagField(value = "arcLenFactors", mapper = DoubleListMapper.class)
        private List<Double> arcLenFactors;
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
            this.rollOffsetType = rollAndOffsetInfo.rollOffsetType;
            this.rollEffectTile = rollAndOffsetInfo.rollEffectTile;
            this.railBlockNormal = rollAndOffsetInfo.railBlockNormal;
            this.degreeMode = rollAndOffsetInfo.degreeMode;
            this.offsetVertByNormal = rollAndOffsetInfo.offsetVertByNormal;

            this.arcLenFactors = new ArrayList<>(rollAndOffsetInfo.arcLenFactors);
            this.rolls = new ArrayList<>(rollAndOffsetInfo.rolls);
            this.rollCtrls = new ArrayList<>(rollAndOffsetInfo.rollCtrls);
            this.yOffsets = new ArrayList<>(rollAndOffsetInfo.yOffsets);
            this.yOffsetCtrls = new ArrayList<>(rollAndOffsetInfo.yOffsetCtrls);
            this.zOffsets = new ArrayList<>(rollAndOffsetInfo.zOffsets);
            this.zOffsetCtrls = new ArrayList<>(rollAndOffsetInfo.zOffsetCtrls);
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            RollAndOffsetInfo defaultInfo = getDefault();
            rollOffsetType = defaultInfo.rollOffsetType;
            rollEffectTile = defaultInfo.rollEffectTile;
            railBlockNormal = defaultInfo.railBlockNormal;
            degreeMode = defaultInfo.degreeMode;
            offsetVertByNormal = defaultInfo.offsetVertByNormal;

            arcLenFactors = defaultInfo.arcLenFactors;
            rolls = defaultInfo.rolls;
            rollCtrls = defaultInfo.rollCtrls;
            yOffsets = defaultInfo.yOffsets;
            yOffsetCtrls = defaultInfo.yOffsetCtrls;
            zOffsets = defaultInfo.zOffsets;
            zOffsetCtrls = defaultInfo.zOffsetCtrls;

            TagSerializer.deserialize(data, this);
        }

        public RollAndOffsetInfo immutable() {
            return new RollAndOffsetInfo(
                    rollOffsetType,
                    rollEffectTile,
                    railBlockNormal,
                    degreeMode,
                    offsetVertByNormal,

                    arcLenFactors,
                    rolls,
                    rollCtrls,
                    yOffsets,
                    yOffsetCtrls,
                    zOffsets,
                    zOffsetCtrls
            );
        }

        //As we stored every point of a curve(2 base points, 2 control points), we will get duplicated arcLenFactor, so we have physical and logical index
        //Physical index: range->[0,point size)
        //Logical index: range->[0,point size/2)
        //Function for Gui config, notice that when using these, Lists CANT be NULL!
        /**
         * This will return -1 if L not in range
         */
        public int findPhysicalIndex(double arcLenFactor) {//return physical index
            int res = -1;
            for (int i = 0; i < arcLenFactors.size(); i += 2) {
                if(Math.abs(arcLenFactors.get(i) - arcLenFactor) < 1e-6) {//this is a place related to control point number limit
                    res = i;
                    break;
                }
            }
            if(!arcLenFactors.isEmpty() && arcLenFactors.getLast() == arcLenFactor) {
                res = arcLenFactors.size()-1;
            }
            return res;
        }

        /**
         * SubSplit the curve surrounded the arcLenFactor if arcLenFactor is in range and not overlapped with existing points
         * */
        public boolean tryInsertBySubSplit(double arcLenFactor) {
            if(findPhysicalIndex(arcLenFactor) != -1) return false;

            List<Pair<Double, Double>> divider = new ArrayList<>();
            divider.add(Pair.of(0d, arcLenFactor));
            divider.add(Pair.of(arcLenFactor, 1d));

            RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(
                    rollOffsetType, rollEffectTile, railBlockNormal, degreeMode, offsetVertByNormal,
                    arcLenFactors, rolls, rollCtrls, yOffsets, yOffsetCtrls, zOffsets, zOffsetCtrls
            );
            List<RollAndOffsetInfo> res = rollAndOffsetInfo.subSplit(divider, false);

            arcLenFactors.clear();
            arcLenFactors.addAll(res.get(0).arcLenFactors);
            arcLenFactors.addAll(res.get(1).arcLenFactors);

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

        /**
         * Merge curves by removing points at arcLenFactor directly
         * */
        public boolean tryDeleteDirectly(double arcLenFactor) {
            int idx = findPhysicalIndex(arcLenFactor);
            if(idx == -1 || idx == 0 || idx == arcLenFactors.size() - 1) return false;

            arcLenFactors.remove(idx - 1);
            arcLenFactors.remove(idx - 1);

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

        /**
         * Remove all points and only set default points
         * */
        public void resetAll() {
            arcLenFactors.clear();
            rolls.clear();
            rollCtrls.clear();
            yOffsets.clear();
            yOffsetCtrls.clear();
            zOffsets.clear();
            zOffsetCtrls.clear();

            arcLenFactors.add(0d); arcLenFactors.add(1d);
            rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
            rollCtrls.add(new Vec3d(1d / 3, 0, 0)); rollCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            yOffsets.add(new Vec3d(0, 0, 0)); yOffsets.add(new Vec3d(1, 0, 0));
            yOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); yOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
            zOffsets.add(new Vec3d(0, 0, 0)); zOffsets.add(new Vec3d(1, 0, 0));
            zOffsetCtrls.add(new Vec3d(1d / 3, 0, 0)); zOffsetCtrls.add(new Vec3d(1 + 1d / 3, 0, 0));
        }

        /**
         * Move points at arcLenFactor in vertical direction
         * */
        public boolean tryDeltaValue(double arcLenFactor, double val, ExtraInfoType type) {
            int idx = findPhysicalIndex(arcLenFactor);
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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return false;
            }

            Vec3d newValue = new Vec3d(points.get(idx).x, points.get(idx).y, val);
            Vec3d oldValue = ctrls.get(idx);
            double delta = val - points.get(idx).z;
            points.set(idx, newValue);
            ctrls.set(idx, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));

            if(idx > 0 && idx < arcLenFactors.size() - 1) {
                oldValue = ctrls.get(idx - 1);
                points.set(idx - 1, newValue);
                ctrls.set(idx - 1, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));
            }

            return true;
        }

        /**
         * Get vertical value at arcLenFactor
         * */
        public String getValueDisplay(double arcLenFactor, ExtraInfoType type) {
            int idx = findPhysicalIndex(arcLenFactor);
            if(idx == -1) return GuiText.NONE.toString();

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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return GuiText.NONE.toString();
            }

            return String.format("%.4f", points.get(idx).z);
        }

        /**
         * Change control point weight
         * */
        public boolean trySetHandleXLen(double arcLenFactor, double val, ExtraInfoType type, boolean editLeft, double length) {
            int idx = findPhysicalIndex(arcLenFactor);
            if(idx == -1) return false;
            if(idx == 0 && editLeft) return false;
            if(idx == arcLenFactors.size() - 1 && !editLeft) return false;
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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return false;
            }

            boolean feedback = false;

            int editIdx;
            double segmentLen;
            if(editLeft) {
                if(idx == arcLenFactors.size() - 1) {
                    editIdx = idx;
                    segmentLen = Math.abs(arcLenFactors.get(editIdx) - arcLenFactors.get(editIdx - 1));
                }else {
                    editIdx = idx - 1;
                    segmentLen = Math.abs(arcLenFactors.get(editIdx) - arcLenFactors.get(editIdx - 1));
                }
            }else {
                editIdx = idx;
                segmentLen = Math.abs(arcLenFactors.get(editIdx) - arcLenFactors.get(editIdx + 1));
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

        /**
         * Get control point weight value for displaying
         * */
        public String getHandleXDisplay(double arcLenFactor, ExtraInfoType type, boolean editLeft, double length) {
            int idx = findPhysicalIndex(arcLenFactor);
            if(idx == -1) return GuiText.NONE.toString();
            if(idx == 0 && editLeft) return GuiText.NONE.toString();
            if(idx == arcLenFactors.size() - 1 && !editLeft) return GuiText.NONE.toString();

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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return GuiText.NONE.toString();
            }

            int displayIdx;
            if(editLeft) {
                if(idx == arcLenFactors.size() - 1) {
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

        public boolean trySetSlope(double arcLenFactor, double val, ExtraInfoType type, double length) {
            int idx = findPhysicalIndex(arcLenFactor);
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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return false;
            }

            double fullZWithLength = val * slopeScale * length;

            Vec3d oldCtrl = ctrls.get(idx);
            double newValueBase = points.get(idx).z;
            double newValueDelta = (ctrls.get(idx).x - points.get(idx).x) * fullZWithLength;
            double newValue = newValueBase + newValueDelta;
            ctrls.set(idx, new Vec3d(oldCtrl.x, oldCtrl.y, newValue));

            if(idx > 0 && idx < arcLenFactors.size() - 1) {
                oldCtrl = ctrls.get(idx - 1);
                newValueBase = points.get(idx - 1).z;
                newValueDelta = (ctrls.get(idx - 1).x - points.get(idx - 1).x) * fullZWithLength;
                newValue = newValueBase + newValueDelta;
                ctrls.set(idx - 1, new Vec3d(oldCtrl.x, oldCtrl.y, newValue));
            }

            return true;
        }
        public String getSlopeDisplay(double arcLenFactor, ExtraInfoType type, double length) {
            int idx = findPhysicalIndex(arcLenFactor);
            if(idx == -1) return GuiText.NONE.toString();

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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return GuiText.NONE.toString();
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
                    ImmersiveRailroading.error("Invalid ExtraInfoType:" + type);
                    return null;
            }

            List<CubicCurve> curves = new ArrayList<>();
            for(int i = 0; i < arcLenFactors.size(); i+=2) {
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

    public enum RollAndVertOffsetAlignType {
        MID(0),
//      HIGHEST(3),
//      LOWEST(4),
        LEFT(1),
        RIGHT(2),
        ;

        private final int order;

        RollAndVertOffsetAlignType(int order){
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        public static RollAndVertOffsetAlignType byOrder(int order) {
            for (RollAndVertOffsetAlignType type : values()) {
                if (type.order == order) {
                    return type;
                }
            }
            return MID;
        }

        @Override
        public String toString() {
            return TextUtil.translate("track.immersiverailroading:roll_vert_offset_align_type." + name().toLowerCase(Locale.ROOT));
        }
    }

    public enum ExtraInfoType {
        ROLL,
        Y_OFFSET,
        Z_OFFSET;

        @Override
        public String toString() {
            return TextUtil.translate("track.immersiverailroading:extra_info_type." + super.toString().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Get pair list (arcLenFactorStart and arcLenFactorEnd) from cubic curve list
     * */
    public static List<Pair<Double,Double>> toRange(List<CubicCurve> subCurves) {
        List<Pair<Double,Double>> res = new ArrayList<>();
        for(CubicCurve subCurve : subCurves) {
            res.add(Pair.of(subCurve.arcLenFactorStart, subCurve.arcLenFactorEnd));
        }
        return res;
    }

    //SubSplit
    //
    //Classified Discussion: analyze based on the number of points contained within the interval from tStart to tEnd.
    //0:Store a segment of line, but both tStart and tEnd require truncating, then normalize and store.
    //More than 0:both tStart and tEnd truncate require truncating, and then normalize and store, points between tStart and tEnd need to be normalized and stored
    //
    //Boundary:
    //Case1:tStart overlay with a point
    //Case2:tEnd overlay with a point
    public List<RollAndOffsetInfo> subSplit(List<Pair<Double,Double>> subCurves, boolean normalize) {//only situation need truncating go into here, but some bug will case invalid tStart and tEnd(overlap), causing arcLenFactors.size()==0
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

            int logicIdxStart = (findRight(arcLenFactors,lStart) + 1) / 2;
            int logicIdxEnd = (findLeft(arcLenFactors,lEnd) + 1) / 2;//physical index => logical index

            int count = logicIdxEnd - logicIdxStart + 1;
            if(count == 0) {//findLeft/Right include situation of equaling, count == 0 is strictly satisfied, also this means results.size() >= 3
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

                {//yOffset
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

                {//zOffset
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
                    //Start
                    if(lStart != arcLenFactors.get(physic(logicIdxStart))) {
                        Vec3d p1 = rolls.get(physic(logicIdxStart - 1));
                        Vec3d p2 = rolls.get(physic(logicIdxStart));
                        Vec3d ctrl1 = rollCtrls.get(physic(logicIdxStart - 1));
                        Vec3d ctrl2 = rolls.get(physic(logicIdxStart)).scale(2).subtract(rollCtrls.get(
                                physic(logicIdxStart)));//The Ctrl is on the right, so Ctrl2 needs to be inverted

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
                    //Mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newT.add(arcLenFactors.get(physic(i)));
                        newT.add(arcLenFactors.get(physic(i + 1)));//will not go into this loop when idxStart == idxEnd
                        newRolls.add(rolls.get(physic(i)));
                        newRolls.add(rolls.get(physic(i + 1)));
                        newRollCtrls.add(rollCtrls.get(physic(i)));
                        newRollCtrls.add(rollCtrls.get(physic(i + 1)));
                    }
                    //End
                    if(lEnd != arcLenFactors.get(physic(logicIdxEnd))) {
                        Vec3d p1 = rolls.get(physic(logicIdxEnd));
                        Vec3d p2 = rolls.get(physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = rollCtrls.get(physic(logicIdxEnd));
                        Vec3d ctrl2 = rolls.get(physic(logicIdxEnd + 1)).scale(2).subtract(rollCtrls.get(
                                physic(logicIdxEnd + 1)));

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
                    //Start
                    if(lStart != arcLenFactors.get(physic(logicIdxStart))) {
                        Vec3d p1 = yOffsets.get(physic(logicIdxStart - 1));
                        Vec3d p2 = yOffsets.get(physic(logicIdxStart));
                        Vec3d ctrl1 = yOffsetCtrls.get(physic(logicIdxStart - 1));
                        Vec3d ctrl2 = yOffsets.get(physic(logicIdxStart)).scale(2).subtract(yOffsetCtrls.get(
                                physic(logicIdxStart)));//The Ctrl is on the right, so Ctrl2 needs to be inverted

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
                    //Mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newYOffsets.add(yOffsets.get(physic(i)));
                        newYOffsets.add(yOffsets.get(physic(i + 1)));
                        newYOffsetCtrls.add(yOffsetCtrls.get(physic(i)));
                        newYOffsetCtrls.add(yOffsetCtrls.get(physic(i + 1)));
                    }
                    //End
                    if(lEnd != arcLenFactors.get(physic(logicIdxEnd))) {
                        Vec3d p1 = yOffsets.get(physic(logicIdxEnd));
                        Vec3d p2 = yOffsets.get(physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = yOffsetCtrls.get(physic(logicIdxEnd));
                        Vec3d ctrl2 = yOffsets.get(physic(logicIdxEnd + 1)).scale(2).subtract(yOffsetCtrls.get(
                                physic(logicIdxEnd + 1)));

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
                    //Start
                    if(lStart != arcLenFactors.get(physic(logicIdxStart))) {
                        Vec3d p1 = zOffsets.get(physic(logicIdxStart - 1));
                        Vec3d p2 = zOffsets.get(physic(logicIdxStart));
                        Vec3d ctrl1 = zOffsetCtrls.get(physic(logicIdxStart - 1));
                        Vec3d ctrl2 = zOffsets.get(physic(logicIdxStart)).scale(2).subtract(zOffsetCtrls.get(
                                physic(logicIdxStart)));//we specify that Ctrl is on the right side of Point, so Ctrl2 must store the reverse.

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
                    //Mid
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newZOffsets.add(zOffsets.get(physic(i)));
                        newZOffsets.add(zOffsets.get(physic(i + 1)));
                        newZOffsetCtrls.add(zOffsetCtrls.get(physic(i)));
                        newZOffsetCtrls.add(zOffsetCtrls.get(physic(i + 1)));
                    }
                    //End
                    if(lEnd != arcLenFactors.get(physic(logicIdxEnd))) {
                        Vec3d p1 = zOffsets.get(physic(logicIdxEnd));
                        Vec3d p2 = zOffsets.get(physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = zOffsetCtrls.get(physic(logicIdxEnd));
                        Vec3d ctrl2 = zOffsets.get(physic(logicIdxEnd + 1)).scale(2).subtract(zOffsetCtrls.get(
                                physic(logicIdxEnd + 1)));

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
                    rollOffsetType, rollEffectTile, railBlockNormal, degreeMode, offsetVertByNormal,
                    newT, newRolls, newRollCtrls, newYOffsets, newYOffsetCtrls, newZOffsets, newZOffsetCtrls,
                    lStart, lEnd
            ));

            else results.add(new RollAndOffsetInfo(
                    rollOffsetType, rollEffectTile, railBlockNormal, degreeMode, offsetVertByNormal,
                    newT, newRolls, newRollCtrls, newYOffsets, newYOffsetCtrls, newZOffsets, newZOffsetCtrls
            ));
        }
        return results;
    }

    private int physic(int logic) {
        if(logic == 0) {
            return 0;
        }
        return Math.min(logic * 2, arcLenFactors.size() - 1);
    }

    /**
     * Split the curve at X (horizonal value) and return the left part of the curve
     * */
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

    /**
     * Get bezier factor t at target X (horizonal value)
     * */
    public static double getTByX(double targetX, CubicCurve curve) {//targetX is X-axis value not Length value!
        if (Math.abs(curve.p2.x - curve.p1.x) < 1e-12) return 0.5;
        double t = (targetX - curve.p1.x) / (curve.p2.x - curve.p1.x);

        for (int i = 0; i < 10; i++) {
            Vec3d pos = curve.position(t);
            double error = pos.x - targetX;
            if (Math.abs(error) < 1e-12) break;

            Vec3d deriv = curve.derivative(t);
            if (Math.abs(deriv.x) < 1e-12) break;

            t = t - error / deriv.x;
            t = Math.clamp(t, 0, 1);
        }

        return t;
    }

    private static RollAndOffsetInfo normalize(
            RollAndVertOffsetAlignType rollOffsetType, boolean rollEffectTile, boolean railBlockNormal, boolean degreeMode, boolean offsetVertByNormal,
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
                rollOffsetType, rollEffectTile, railBlockNormal, degreeMode, offsetVertByNormal,
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
        return getValue(this.arcLenFactors, l, rolls, rollCtrls);
    }

    /**
     * Used to correct Rail part pitch in BuilderIterator
     */
    public double getRelRollSlopeStart(double length, boolean isRight, double gauge) {
        int idx = 0;

        double tan;

        if (degreeMode) {
            double dRoll = Math.toRadians(rollCtrls.get(idx).z - rolls.get(idx).z);
            double ds = (rollCtrls.get(idx).x - rolls.get(idx).x) * length;

            tan = dRoll * (gauge * 0.5) / ds;
        } else {
            tan = (rollCtrls.get(idx).z - rolls.get(idx).z) / (rollCtrls.get(idx).x - rolls.get(idx).x);
            tan /= length;
            tan *= 0.01;
            tan *= 0.5;
        }
        tan = isRight ? -tan : tan;

        return -Math.toDegrees(Math.atan(tan));
    }

    /**
     * Used to correct Rail part pitch in BuilderIterator
     */
    public double getRelRollSlopeEnd(double length, boolean isRight, double gauge) {
        int idx = arcLenFactors.size() - 1;

        double tan;

        if (degreeMode) {
            double dRoll = Math.toRadians(rollCtrls.get(idx).z - rolls.get(idx).z);
            double ds = (rollCtrls.get(idx).x - rolls.get(idx).x) * length;

            tan = dRoll * (gauge * 0.5) / ds;
        } else {
            tan = (rollCtrls.get(idx).z - rolls.get(idx).z) / (rollCtrls.get(idx).x - rolls.get(idx).x);
            tan /= length;
            tan *= 0.01;
            tan *= 0.5;
        }
        tan = isRight ? -tan : tan;

        return -Math.toDegrees(Math.atan(tan));
    }

    public double getYOffset(double l) {
        return getValue(this.arcLenFactors, l, yOffsets, yOffsetCtrls);
    }

    public double getZOffset(double l) {
        return getValue(this.arcLenFactors, l, zOffsets, zOffsetCtrls);
    }

    /**
     * Used to correct Rail part pitch in BuilderIterator
     */
    public double getZOffsetSlopeStart(double length) {
        int idx = 0;
        double tan = (zOffsetCtrls.get(idx).z - zOffsets.get(idx).z) / (zOffsetCtrls.get(idx).x - zOffsets.get(idx).x);
        tan /= length;
        return -Math.toDegrees(Math.atan(tan));
    }

    /**
     * Used to correct Rail part pitch in BuilderIterator
     */
    public double getZOffsetSlopeEnd(double length) {
        int idx = arcLenFactors.size() - 1;
        double tan = (zOffsetCtrls.get(idx).z - zOffsets.get(idx).z) / (zOffsetCtrls.get(idx).x - zOffsets.get(idx).x);
        tan /= length;
        return -Math.toDegrees(Math.atan(tan));
    }

    /**
     * Get vertical value at target X (horizonal value)
     * */
    public static double getValue(List<Double>arcLenFactors, double targetX, List<Vec3d> points, List<Vec3d> ctrls) {
        // notice that segmentIdx is not point index!
        int segmentIdx = findValidSegment(targetX, arcLenFactors);

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

    /**
     * Find the left point index of curve segment surrounding arcLenFactors
     * */
    public static int findValidSegment(double targetArcLenFactor, List<Double> arcLenFactors) {
        int n = arcLenFactors.size() / 2;
        if (n == 0) return -1;

        if (targetArcLenFactor <= arcLenFactors.getFirst()) return 0;
        if (targetArcLenFactor >= arcLenFactors.getLast()) return n - 1;

        int left = 0, right = n - 1;
        int idx = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            double start = arcLenFactors.get(mid * 2);
            if (start <= targetArcLenFactor) {
                idx = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return idx;
    }
}
