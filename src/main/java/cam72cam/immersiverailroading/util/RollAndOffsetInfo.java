package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;

@TagMapped(RollAndOffsetInfo.TagMapper.class)
public class RollAndOffsetInfo {
    //TODO:roll and offset
    // 先完成存储和gui（可能是是零时的），再来完成此处的计算逻辑，(需要验证检查)
    // 再接下来补充寻路逻辑（还没写）
    public final List<Double> xs;
    //Roll
    public final List<Vec3d> rolls;
    public final List<Vec3d> rollCtrls;
    //Y Offset
    public final List<Vec3d> offsets;
    public final List<Vec3d> offsetCtrls;

    public RollAndOffsetInfo(List<Double> t, List<Vec3d> rolls, List<Vec3d> rollCtrls, List<Vec3d> offsets, List<Vec3d> offsetlCtrls) {
        this.xs = t;
        this.rolls = rolls;
        this.rollCtrls = rollCtrls;
        this.offsets = offsets;
        this.offsetCtrls = offsetlCtrls;
    }

    public static class Mutable {
        @TagField(value = "xs", mapper = DoubleListMapper.class)
        public List<Double> xs;
        @TagField(value = "rolls", mapper = Vec3dListMapper.class)
        public List<Vec3d> rolls;
        @TagField(value = "rollCtrls", mapper = Vec3dListMapper.class)
        public List<Vec3d> rollCtrls;
        @TagField(value = "offsets", mapper = Vec3dListMapper.class)
        public List<Vec3d> offsets;
        @TagField(value = "offsetCtrls", mapper = Vec3dListMapper.class)
        public List<Vec3d> offsetCtrls;

        public Mutable(RollAndOffsetInfo rollAndOffsetInfo) {
            this.xs = rollAndOffsetInfo.xs;
            this.rolls = rollAndOffsetInfo.rolls;
            this.rollCtrls = rollAndOffsetInfo.rollCtrls;
            this.offsets = rollAndOffsetInfo.offsets;
            this.offsetCtrls = rollAndOffsetInfo.offsetCtrls;
        }

        public Mutable(TagCompound data) throws SerializationException {
            // Defaults
            xs = new ArrayList<>();
            rolls = new ArrayList<>();
            rollCtrls = new ArrayList<>();
            offsets = new ArrayList<>();
            offsetCtrls = new ArrayList<>();

            xs.add(0d); xs.add(1d);
            rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
            rollCtrls.add(new Vec3d(0.25, 0, 0)); rollCtrls.add(new Vec3d(1.25, 0, 0));
            offsets.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
            offsetCtrls.add(new Vec3d(0.25, 0, 0)); offsetCtrls.add(new Vec3d(1.25, 0, 0));

            TagSerializer.deserialize(data, this);
        }

        public RollAndOffsetInfo immutable() {
            return new RollAndOffsetInfo(
                    xs,
                    rolls,
                    rollCtrls,
                    offsets,
                    offsetCtrls
            );
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

    public static List<Pair<Double,Double>> curvesToTRange(List<CubicCurve> subCurves) {
        List<Pair<Double,Double>> res = new ArrayList<>();
        for(CubicCurve subCurve : subCurves) {
            res.add(Pair.of(subCurve.xStart, subCurve.xEnd));
        }
        return res;
    }

    //subSplit
    //分类讨论:按照tStart到tEnd区间包含的t的点个数讨论
    //0个:new内存一段线即可，但是tStart和tEnd两端都需要计算，再参数化到01存储->ok
    //非0个:tStart和tEnd两端也都需要计算再参数化到01存储，中间的t就只要参数化到01
    //其中边界条件:
    //1:tStart和点重合
    //2:tEnd和点重合
    public List<RollAndOffsetInfo> subSplit(List<Pair<Double,Double>> subCurves, boolean normalize) {//只有分段发生才会进入此处
        List<RollAndOffsetInfo> results = new ArrayList<>();

        for (Pair<Double,Double> subCurve : subCurves) {
            double tStart = subCurve.getLeft();
            double tEnd = subCurve.getRight();

            List<Double> newT = new ArrayList<>();
            List<Vec3d> newRolls = new ArrayList<>();
            List<Vec3d> newRollCtrls = new ArrayList<>();
            List<Vec3d> newOffsets = new ArrayList<>();
            List<Vec3d> newOffsetCtrls = new ArrayList<>();

            int logicIdxStart = (findRight(xs,tStart) + 1) / 2;//这两个查找方法（最好二分？）等于时都满足
            int logicIdxEnd = (findLeft(xs,tEnd) + 1) / 2;//搜到的是点对应的x是对的,处理成逻辑上的index

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
                    CubicCurve startCurve = curve.truncateByX(tStart);
                    CubicCurve endCurve = curve.truncateByX(tEnd);

                    p1 = startCurve.p2;
                    p2 = endCurve.p2;
                    ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);
                    ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                    newRolls.add(p1);
                    newRolls.add(p2);
                    newRollCtrls.add(ctrl1);
                    newRollCtrls.add(ctrl2);
                }

                {//offset
                    Vec3d p1 = offsets.get(logicIdxEnd * 2);
                    Vec3d p2 = offsets.get(logicIdxStart * 2 - 1);
                    Vec3d ctrl1 = offsetCtrls.get(logicIdxEnd * 2);
                    Vec3d ctrl2 = offsets.get(logicIdxStart * 2 - 1).scale(2).subtract(offsetCtrls.get(logicIdxStart * 2 - 1));

                    CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                    CubicCurve startCurve = curve.truncateByX(tStart);
                    CubicCurve endCurve = curve.truncateByX(tEnd);

                    p1 = startCurve.p2;
                    p2 = endCurve.p2;
                    ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);
                    ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                    newOffsets.add(p1);
                    newOffsets.add(p2);
                    newOffsetCtrls.add(ctrl1);
                    newOffsetCtrls.add(ctrl2);
                }
            }else {
                //avoid duplicated points? but we can retain them to draw G1 continuity(which shares only ctrl angle), not limited to C1(which shares both ctrl angle and ctrl length)
//                int startPointAmount = 0;
//                int midPointAmount = 0;
//                int endPointAmount = 0;
                {//roll
                    //起点段
                    if(tStart != xs.get(Physic(logicIdxStart))) {
                        Vec3d p1 = rolls.get(Physic(logicIdxStart - 1));
                        Vec3d p2 = rolls.get(Physic(logicIdxStart));
                        Vec3d ctrl1 = rollCtrls.get(Physic(logicIdxStart - 1));
                        Vec3d ctrl2 = rolls.get(Physic(logicIdxStart)).scale(2).subtract(rollCtrls.get(Physic(logicIdxStart)));//规定ctrl在右边，所以ctrl2都要取反一下

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve startCurve = curve.truncateByX(tStart);
                        p1 = startCurve.p2;
                        ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);

                        newT.add(p1.x);
//                        newT.add(tStart);//force
                        newT.add(p2.x);
                        newRolls.add(p1);
//                        newRolls.add(new Vec3d(tStart, p1.y, p1.z));//force
                        newRolls.add(p2);
                        newRollCtrls.add(ctrl1);
                        newRollCtrls.add(ctrl2);
                    }
                    //中间
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newT.add(xs.get(Physic(i)));
                        newT.add(xs.get(Physic(i + 1)));//当idxStart==idxEnd时不会进入循环，不会越界
                        newRolls.add(rolls.get(Physic(i)));
                        newRolls.add(rolls.get(Physic(i + 1)));
                        newRollCtrls.add(rollCtrls.get(Physic(i)));
                        newRollCtrls.add(rollCtrls.get(Physic(i + 1)));
                    }
                    //终点段
                    if(tEnd != xs.get(Physic(logicIdxEnd))) {
                        Vec3d p1 = rolls.get(Physic(logicIdxEnd));
                        Vec3d p2 = rolls.get(Physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = rollCtrls.get(Physic(logicIdxEnd));
                        Vec3d ctrl2 = rolls.get(Physic(logicIdxEnd + 1)).scale(2).subtract(rollCtrls.get(Physic(logicIdxEnd + 1)));

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve endCurve = curve.truncateByX(tEnd);//todo:truncate不对，并不会成比例，另外截断要做到完美保留tStart? 不过t要改名x了，还得写一个好的插值方式
                        p2 = endCurve.p2;
                        ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                        newT.add(p1.x);
                        newT.add(p2.x);
//                        newT.add(tEnd);//force
                        newRolls.add(p1);
                        newRolls.add(p2);
//                        newRolls.add(new Vec3d(tEnd, p2.y, p2.z));//force
                        newRollCtrls.add(ctrl1);
                        newRollCtrls.add(ctrl2);
                    }
                }

                {//offset
                    //起点段
                    if(tStart != xs.get(Physic(logicIdxStart))) {
                        Vec3d p1 = offsets.get(Physic(logicIdxStart - 1));
                        Vec3d p2 = offsets.get(Physic(logicIdxStart));
                        Vec3d ctrl1 = offsetCtrls.get(Physic(logicIdxStart - 1));
                        Vec3d ctrl2 = offsets.get(Physic(logicIdxStart)).scale(2).subtract(offsetCtrls.get(Physic(logicIdxStart)));//规定ctrl在右边，所以ctrl2都要取反一下

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve startCurve = curve.truncateByX(tStart);
                        p1 = startCurve.p2;
                        ctrl1 = startCurve.p2.scale(2).subtract(startCurve.ctrl2);

                        newOffsets.add(p1);
//                        newOffsets.add(new Vec3d(tEnd, p1.y, p1.z));//force
                        newOffsets.add(p2);
                        newOffsetCtrls.add(ctrl1);
                        newOffsetCtrls.add(ctrl2);
                    }
                    //中间
                    for(int i = logicIdxStart; i < logicIdxEnd; i ++) {
                        newOffsets.add(offsets.get(Physic(i)));
                        newOffsets.add(offsets.get(Physic(i + 1)));
                        newOffsetCtrls.add(offsetCtrls.get(Physic(i)));
                        newOffsetCtrls.add(offsetCtrls.get(Physic(i + 1)));
                    }
                    //终点段
                    if(tEnd != xs.get(Physic(logicIdxEnd))) {
                        Vec3d p1 = offsets.get(Physic(logicIdxEnd));
                        Vec3d p2 = offsets.get(Physic(logicIdxEnd + 1));
                        Vec3d ctrl1 = offsetCtrls.get(Physic(logicIdxEnd));
                        Vec3d ctrl2 = offsets.get(Physic(logicIdxEnd + 1)).scale(2).subtract(offsetCtrls.get(Physic(logicIdxEnd + 1)));

                        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
                        CubicCurve endCurve = curve.truncateByX(tEnd);
                        p2 = endCurve.p2;
                        ctrl2 = endCurve.p2.scale(2).subtract(endCurve.ctrl2);

                        newOffsets.add(p1);
                        newOffsets.add(p2);
//                        newOffsets.add(new Vec3d(tEnd, p2.y, p2.z));//force
                        newOffsetCtrls.add(ctrl1);
                        newOffsetCtrls.add(ctrl2);
                    }
                }
            }
            if(normalize) results.add(normalize(newT, newRolls, newRollCtrls, newOffsets, newOffsetCtrls, tStart, tEnd));
            else results.add(new RollAndOffsetInfo(newT, newRolls, newRollCtrls, newOffsets, newOffsetCtrls));
        }
        return results;
    }

    private int Physic(int logic) {
        if(logic == 0) return 0;
        if(logic * 2 > xs.size() - 1) return xs.size() - 1;
        return logic * 2;
    }

    private static RollAndOffsetInfo normalize(List<Double> newT, List<Vec3d> newRolls, List<Vec3d> newRollCtrls, List<Vec3d> newOffsets, List<Vec3d> newOffsetCtrls, double tStart, double tEnd) {

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

            //offsets
            Vec3d oldOffset = newOffsets.get(i);
            newOffsets.set(i, new Vec3d(newTi, oldOffset.y, oldOffset.z));
            Vec3d oldOffsetCtrl = newOffsetCtrls.get(i);
            double newOffsetCtrlX = (oldOffsetCtrl.x - tStart) / (tEnd - tStart);
            newOffsetCtrls.set(i, new Vec3d(newOffsetCtrlX, oldOffsetCtrl.y, oldOffsetCtrl.z));
        }
        return new RollAndOffsetInfo(newT, newRolls, newRollCtrls, newOffsets, newOffsetCtrls);
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

    public double getRoll(double t) {
        return interpolateValue(this.xs, t, rolls, rollCtrls);
    }

    public double getOffset(double t) {
        return interpolateValue(this.xs, t, offsets, offsetCtrls);
    }

    public static double interpolateValue(List<Double>t, double targetT, List<Vec3d> points, List<Vec3d> ctrls) {
        // notice that segmentIdx is not point index!
        int segmentIdx = findSegment(targetT, t);
        if (segmentIdx < 0) segmentIdx = 0;
        int maxSegment = t.size() / 2 - 1;
        if (segmentIdx > maxSegment) segmentIdx = maxSegment;

        // 每段两个点
        int p1Idx = segmentIdx * 2;
        int p2Idx = p1Idx + 1;

        // 获取点和控制点
        Vec3d p1 = points.get(p1Idx);
        Vec3d p2 = points.get(p2Idx);
        Vec3d ctrl1 = ctrls.get(p1Idx);
        Vec3d ctrl2Forward = ctrls.get(p2Idx);
        Vec3d ctrl2 = p2.scale(2).subtract(ctrl2Forward);

        // 计算局部参数
        double t0 = t.get(p1Idx);
        double t1 = t.get(p2Idx);
        double localT = (targetT - t0) / (t1 - t0);

        // 插值
        CubicCurve curve = new CubicCurve(p1, ctrl1, ctrl2, p2);
        Vec3d pos = curve.position(localT);

        return pos.z;
    }

    public static int findSegment(double targetT, List<Double> t) {
        // 遍历每段 [t[2*i], t[2*i+1]]
        for (int i = 0; i < t.size() / 2; i++) {
            double segStart = t.get(i * 2);
            double segEnd = t.get(i * 2 + 1);
            if (targetT >= segStart && targetT <= segEnd) {
                return i;
            }
        }
        // 边界
        if (targetT <= t.get(0)) return 0;
        return t.size() / 2 - 1;
    }
}
