package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackSmoothing;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.BuilderCubicCurve;
import cam72cam.immersiverailroading.track.CubicCurve;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.util.*;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TrackSingleGeometrySegment {
    // 物理绝对坐标（对于转弯，这是子方块位置；普通段就是自己）
    public final Vec3i absPos;
    // 拓扑归属坐标（永远指向所在的 TrackMultiGeometrySegment 的中心坐标，用于防误删）
    public final Vec3i parentAbsPos;

    public final Gauge gauge;
    public final int switchIdex;
    public final float yaw;
    public final Vec3d placementPosition;
    public final CubicCurve baseCurve;
    public final RollAndOffsetInfo rollAndOffsetInfo;
    public final String referenceTrack;// for path piece height
    public final TrackFaceTransSetting.FacePivotType facePivotType;
    public final Vec3d facePivotOffset;

    public final HashSet<Vec3i> positionsCache;
    public final List<VecYPR> pointsCache;

    private BuilderCubicCurve builderCache;
    private World builderWorldCache;

    public TrackSingleGeometrySegment(TileRail trackBlock, World world, int switchIndex, Gauge gauge) {
        if(trackBlock.info.settings.type == TrackItems.SWITCH && switchIndex == 1) {
            this.absPos = trackBlock.subInfoPos.get(0);
        } else {
            this.absPos = trackBlock.getPos();
        }
        // 记录父方块坐标，用于 topo 精准清理
        this.parentAbsPos = trackBlock.getPos();

        this.gauge = gauge;
        this.switchIdex = switchIndex;

        BuilderCubicCurve builder;
        if(trackBlock.info.settings.type == TrackItems.SWITCH && switchIndex == 0) { // legacy switch straight way
            builder = (BuilderCubicCurve) trackBlock.info.withSettings(mutable -> mutable.type = TrackItems.STRAIGHT).getBuilder(world, trackBlock.getPos());
        } else if(trackBlock.info.settings.type == TrackItems.SWITCH && switchIndex == 1) {
            builder = (BuilderCubicCurve) trackBlock.subInfos.get(0).getBuilder(world, trackBlock.subInfoPos.get(0));
        } else if(trackBlock.info.settings.type == TrackItems.TURNTABLE) {
            builder = (BuilderCubicCurve) trackBlock.info.withSettings(mutable -> mutable.type = TrackItems.STRAIGHT).getBuilder(world, trackBlock.getPos());
        } else if(trackBlock.info.settings.type == TrackItems.TRANSFERTABLE) {
            builder = (BuilderCubicCurve) trackBlock.info.withSettings(mutable -> mutable.type = TrackItems.STRAIGHT).getBuilder(world, trackBlock.getPos());
        } else{ // common way and legacy switch non-straight way
            builder = (BuilderCubicCurve) trackBlock.info.getBuilder(world, trackBlock.getPos());
        }

        this.yaw = builder.info.placementInfo.yaw;
        this.placementPosition = builder.info.placementInfo.placementPosition;
        this.baseCurve = builder.getCurve();
        this.rollAndOffsetInfo = builder.info.settings.rollAndOffsetInfo;
        this.referenceTrack = builder.info.settings.track;
        this.facePivotType = builder.info.settings.trackFaceTransSetting.facePivotType();
        this.facePivotOffset = builder.info.settings.trackFaceTransSetting.facePivotOffset();

        this.positionsCache = builder.positionsCache;
        this.pointsCache = builder.getPath(0.25 * gauge.scale());
    }

    public Vec3i getBlockPos() {
        return absPos;
    }

    public TrackSingleGeometrySegment(ByteBuffer buffer, World world) {
        int version = buffer.getInt(); // version
        if (version != 1) {
            throw new RuntimeException(String.format("Invalid single track geometry segment data version %d", version));
        }

        // 读取绝对坐标
        this.absPos = new Vec3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
        // 读取父方块坐标
        this.parentAbsPos = new Vec3i(buffer.getInt(), buffer.getInt(), buffer.getInt());

        double x,y,z;

        switchIdex = buffer.getInt(); // switch index
        gauge = Gauge.from(buffer.getDouble()); // gauge
        referenceTrack = TrackRegionUtil.readString(buffer);// referenceTrack

        yaw = buffer.getFloat(); // yaw
        // placementPosition
        x = buffer.getDouble();
        y = buffer.getDouble();
        z = buffer.getDouble();
        placementPosition = new Vec3d(x, y, z);

        // baseCurve p1.xyz ctrl1.xyz ctrl2.xyz p2.xyz
        double[] baseCurveArgs = new double[3 * 4];
        for(int i = 0; i < 3 * 4; i++) {
            baseCurveArgs[i] = buffer.getDouble();
        }
        baseCurve = new CubicCurve(
                new Vec3d(baseCurveArgs[0], baseCurveArgs[1], baseCurveArgs[2]),
                new Vec3d(baseCurveArgs[3], baseCurveArgs[4], baseCurveArgs[5]),
                new Vec3d(baseCurveArgs[6], baseCurveArgs[7], baseCurveArgs[8]),
                new Vec3d(baseCurveArgs[9], baseCurveArgs[10], baseCurveArgs[11])
        );

        facePivotType = TrackFaceTransSetting.FacePivotType.byOrder(buffer.getInt()); // facePivotType
        // facePivotOffset
        x = buffer.getDouble();
        y = buffer.getDouble();
        z = buffer.getDouble();
        facePivotOffset = new Vec3d(x, y, z);

        RollAndOffsetInfo.RollAndVertOffsetAlignType rollOffsetType = RollAndOffsetInfo.RollAndVertOffsetAlignType.byOrder(buffer.getInt()); // rollAndOffsetInfo.rollOffsetType
        boolean degreeMode = buffer.getShort() == 1; // rollAndOffsetInfo.degreeMode
        boolean offsetVertByNormal = buffer.getShort() == 1; // rollAndOffsetInfo.offsetVertByNormal

        int count = buffer.getInt(); // rollAndOffsetInfo.arcLenFactors count

        List<Double> arcLenFactors = new ArrayList<>();
        List<Vec3d> rolls = new ArrayList<>();
        List<Vec3d> rollCtrls = new ArrayList<>();
        List<Vec3d> yOffsets = new ArrayList<>();
        List<Vec3d> yOffsetCtrls = new ArrayList<>();
        List<Vec3d> zOffsets = new ArrayList<>();
        List<Vec3d> zOffsetCtrls = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            arcLenFactors.add(buffer.getDouble()); // rollAndOffsetInfo.arcLenFactors

            // rollAndOffsetInfo.rolls.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            rolls.add(new Vec3d(x, y, z));
            // rollAndOffsetInfo.rollCtrls.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            rollCtrls.add(new Vec3d(x, y, z));
            // rollAndOffsetInfo.yOffsets.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            yOffsets.add(new Vec3d(x, y, z));
            // rollAndOffsetInfo.yOffsetCtrls.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            yOffsetCtrls.add(new Vec3d(x, y, z));
            // rollAndOffsetInfo.zOffsets.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            zOffsets.add(new Vec3d(x, y, z));
            // rollAndOffsetInfo.zOffsetCtrls.xyz
            x = buffer.getDouble();
            y = buffer.getDouble();
            z = buffer.getDouble();
            zOffsetCtrls.add(new Vec3d(x, y, z));
        }

        rollAndOffsetInfo = new RollAndOffsetInfo(
                rollOffsetType, false, false, degreeMode, offsetVertByNormal,
                arcLenFactors, rolls, rollCtrls, yOffsets, yOffsetCtrls, zOffsets, zOffsetCtrls
        );

        positionsCache = getAndUpdateBuilder(world).positionsCache;
        pointsCache = getAndUpdateBuilder(world).getPath(0.25 * gauge.scale());
    }

    public int sizeBytes() {
        int bytes = 0;

        bytes += Integer.BYTES; // version
        bytes += Integer.BYTES * 3; // absPos x,y,z
        bytes += Integer.BYTES * 3; // parentAbsPos x,y,z
        bytes += Integer.BYTES; // switch index
        bytes += Double.BYTES; // gauge
        bytes += TrackRegionUtil.sizeString(referenceTrack); // referenceTrack
        bytes += Integer.BYTES; // facePivotType
        bytes += Double.BYTES * 3; // facePivotOffset

        bytes += Float.BYTES; // yaw
        bytes += Double.BYTES * 3; // placementPosition
        // baseCurve p1.xyz ctrl1.xyz ctrl2.xyz p2.xyz
        bytes += Double.BYTES * 3 * 4;

        bytes += Integer.BYTES; // rollAndOffsetInfo.rollOffsetType
        bytes += Short.BYTES; // rollAndOffsetInfo.degreeMode
        bytes += Short.BYTES; // rollAndOffsetInfo.offsetVertByNormal

        bytes += Integer.BYTES; // rollAndOffsetInfo.arcLenFactors count
        for(int i = 0; i < rollAndOffsetInfo.arcLenFactors().size(); i++) {
            bytes += Double.BYTES; // rollAndOffsetInfo.arcLenFactors
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.rolls.xyz
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.rollCtrls.xyz
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.yOffsets.xyz
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.yOffsetCtrls.xyz
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.zOffsets.xyz
            bytes += Double.BYTES * 3; // rollAndOffsetInfo.zOffsetCtrls.xyz
        }

        return bytes;
    }

    public void write(ByteBuffer buffer) {
        buffer.putInt(1); // version

        // 写入绝对坐标
        buffer.putInt(absPos.x);
        buffer.putInt(absPos.y);
        buffer.putInt(absPos.z);
        // 写入父方块坐标
        buffer.putInt(parentAbsPos.x);
        buffer.putInt(parentAbsPos.y);
        buffer.putInt(parentAbsPos.z);

        buffer.putInt(switchIdex); // switch index
        buffer.putDouble(gauge.value()); // gauge
        TrackRegionUtil.writeString(referenceTrack, buffer);// referenceTrack

        buffer.putFloat(yaw); // yaw
        // placementPosition
        buffer.putDouble(placementPosition.x);
        buffer.putDouble(placementPosition.y);
        buffer.putDouble(placementPosition.z);
        // baseCurve p1.xyz ctrl1.xyz ctrl2.xyz p2.xyz
        buffer.putDouble(baseCurve.p1.x);
        buffer.putDouble(baseCurve.p1.y);
        buffer.putDouble(baseCurve.p1.z);
        buffer.putDouble(baseCurve.ctrl1.x);
        buffer.putDouble(baseCurve.ctrl1.y);
        buffer.putDouble(baseCurve.ctrl1.z);
        buffer.putDouble(baseCurve.ctrl2.x);
        buffer.putDouble(baseCurve.ctrl2.y);
        buffer.putDouble(baseCurve.ctrl2.z);
        buffer.putDouble(baseCurve.p2.x);
        buffer.putDouble(baseCurve.p2.y);
        buffer.putDouble(baseCurve.p2.z);

        buffer.putInt(facePivotType.ordinal()); // facePivotType
        // facePivotOffset
        buffer.putDouble(facePivotOffset.x);
        buffer.putDouble(facePivotOffset.y);
        buffer.putDouble(facePivotOffset.z);

        buffer.putInt(rollAndOffsetInfo.rollOffsetType().ordinal()); // rollAndOffsetInfo.rollOffsetType
        buffer.putShort(rollAndOffsetInfo.degreeMode() ? (short)1 : (short)0); // rollAndOffsetInfo.degreeMode
        buffer.putShort(rollAndOffsetInfo.offsetVertByNormal() ? (short)1 : (short)0); // rollAndOffsetInfo.offsetVertByNormal

        buffer.putInt(rollAndOffsetInfo.arcLenFactors().size()); // rollAndOffsetInfo.arcLenFactors count
        for(int i = 0; i <rollAndOffsetInfo.arcLenFactors().size(); i++) {
            buffer.putDouble(rollAndOffsetInfo.arcLenFactors().get(i)); // rollAndOffsetInfo.arcLenFactors
            // rollAndOffsetInfo.rolls.xyz
            buffer.putDouble(rollAndOffsetInfo.rolls().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.rolls().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.rolls().get(i).z);
            // rollAndOffsetInfo.rollCtrls.xyz
            buffer.putDouble(rollAndOffsetInfo.rollCtrls().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.rollCtrls().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.rollCtrls().get(i).z);
            // rollAndOffsetInfo.yOffsets.xyz
            buffer.putDouble(rollAndOffsetInfo.yOffsets().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.yOffsets().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.yOffsets().get(i).z);
            // rollAndOffsetInfo.yOffsetCtrls.xyz
            buffer.putDouble(rollAndOffsetInfo.yOffsetCtrls().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.yOffsetCtrls().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.yOffsetCtrls().get(i).z);
            // rollAndOffsetInfo.zOffsets.xyz
            buffer.putDouble(rollAndOffsetInfo.zOffsets().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.zOffsets().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.zOffsets().get(i).z);
            // rollAndOffsetInfo.zOffsetCtrls.xyz
            buffer.putDouble(rollAndOffsetInfo.zOffsetCtrls().get(i).x);
            buffer.putDouble(rollAndOffsetInfo.zOffsetCtrls().get(i).y);
            buffer.putDouble(rollAndOffsetInfo.zOffsetCtrls().get(i).z);
        }
    }

    public BuilderCubicCurve getAndUpdateBuilder(World world) {
        if (builderCache != null && builderWorldCache == world) {
            return builderCache;
        }

        RailSettings settings = new RailSettings(
                gauge, referenceTrack,
                TrackItems.CUSTOM, TrackItems.CUSTOM,
                10, 90, 1, TrackSmoothing.NEITHER,
                new EndPointData(0), new EndPointData(0),
                rollAndOffsetInfo, rollAndOffsetInfo,
                TrackDirection.NONE, new TrackFaceTransSetting(0.1f, facePivotType, facePivotOffset),
                Fuzzy.DIRT.example(), ItemStack.EMPTY,
                false, false,
                0, 0//todo
        );
        RailInfo info = new RailInfo(
                settings,
                new PlacementInfo(baseCurve.p1, TrackDirection.NONE, yaw, baseCurve.ctrl1).offset(placementPosition),
                new PlacementInfo(baseCurve.p2, TrackDirection.NONE, yaw, baseCurve.ctrl2).offset(placementPosition)
        );
        // 使用 absPos
        builderCache = new BuilderCubicCurve(info, world, absPos, false);
        builderWorldCache = world;
        return builderCache;
    }
}