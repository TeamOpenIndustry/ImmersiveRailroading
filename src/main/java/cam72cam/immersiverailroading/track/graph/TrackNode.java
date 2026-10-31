package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackNode {
    public final TrackNodeId id;
    private final long regionPos;// addition, not necessary
    private final Vec3i regionBlockPos;// addition, not necessary

    public final TrackSingleGeometrySegment trackSingleGeometrySegment;
    public final int index;

    public final VecYPR point;

    public TrackNode(TrackSingleGeometrySegment segment, int index, World world) {
        // 强制使用 segment 自身的真实坐标
        Vec3i pos = segment.getAbsPos();
        this.regionPos = TrackRegionUtil.vecToRegion(pos);
        this.regionBlockPos = TrackRegionUtil.toRegionBlockPos(pos);

        this.trackSingleGeometrySegment = segment;
        this.index = index;
        this.point = segment.pointsCache.get(index)
                .add(new Vec3d(pos))
                .add(segment.getAndUpdateBuilder(world).info.placementInfo.placementPosition);
        this.id = new TrackNodeId(regionPos, regionBlockPos, trackSingleGeometrySegment.switchIdex, trackSingleGeometrySegment.gauge, index);
    }

    public TrackNode(TrackSingleGeometrySegment segment, boolean isStart, World world) {
        this(segment, isStart ? 0 : segment.pointsCache.size() - 1, world);
    }

    public TrackNode getConn(World world, boolean forward) {
        if (world == null) return null;

        WorldData data = WorldData.get(world);
        if (data == null) return null;

        int hori = Math.max((int) (trackSingleGeometrySegment.gauge.scale() * 2), 1);
        int vert = 1;
        TrackNode best = null;
        double bestScore = Double.MAX_VALUE;

        // 允许的最大连线夹角（30 度）
        final double maxAngleRad = Math.toRadians(30);
        final double cosCone = Math.cos(maxAngleRad);

        // A 节点的搜索方向（向前或向后）
        Vec3d forwardVec = forwardVector(this.point);
        Vec3d searchVec = forward ? forwardVec : forwardVec.scale(-1);

        Vec3i selfBlock = TrackRegionUtil.toBlockPos(regionPos, regionBlockPos);

        for (int x = -hori; x <= hori; x++) {
            for (int y = -vert; y <= vert; y++) {
                for (int z = -hori; z <= hori; z++) {
                    Vec3i scan = new Vec3i(this.point).add(x, y, z);
                    List<Vec3i> parents = data.getParentsAt(scan);
                    if (parents == null) continue;

                    for (Vec3i parentPos : new ArrayList<>(parents)) { // we have to copy to avoid CME!
                        if (parentPos.equals(selfBlock)) continue;

                        TrackMultiGeometrySegment trackBlock = data.getTrackBlock(parentPos);
                        if (trackBlock == null) {
                            data.ensureRegionLoaded(TrackRegionUtil.vecToRegion(parentPos));
                            trackBlock = data.getTrackBlock(parentPos);
                        }
                        if (trackBlock == null) continue;

                        for (Map<Gauge, TrackSingleGeometrySegment> branch : trackBlock.paths) {
                            for (Map.Entry<Gauge, TrackSingleGeometrySegment> single : branch.entrySet()) {
                                TrackSingleGeometrySegment seg = single.getValue();

                                if (seg.gauge != this.trackSingleGeometrySegment.gauge) continue;

                                List<VecYPR> points = seg.pointsCache;
                                double maxDist = seg.gauge.value();
                                int lastIdx = points.size() - 1;

                                for (int i = 0; i < points.size(); i++) {
                                    VecYPR cand = points.get(i)
                                            .add(new Vec3d(seg.getAbsPos()))
                                            .add(seg.getAndUpdateBuilder(world).info.placementInfo.placementPosition)
                                            .add(new Vec3d(0, seg.getAndUpdateBuilder(world).info.getTrackHeight() * seg.gauge.scale(), 0));

                                    // 1. 距离门槛过滤（仅作为硬拦截）
                                    double d = cand.distanceTo(this.point);
                                    if (d >= maxDist) continue;

                                    // 2. 重合节点直接返回（排除自己）
                                    if (d < 1e-3) {
                                        if (seg == this.trackSingleGeometrySegment && i == this.index) {
                                            continue;
                                        }
                                        return new TrackNode(seg, i, world);
                                    }

                                    // 3. 计算两点连线方向
                                    Vec3d offset = new Vec3d(
                                            cand.x - this.point.x,
                                            cand.y - this.point.y,
                                            cand.z - this.point.z
                                    );
                                    double dist = offset.length();
                                    Vec3d lineDir = offset.scale(1.0 / dist);

                                    // 4. 评估 A 端夹角：连线方向与 A 的搜索方向的夹角
                                    double dotLineA = lineDir.dotProduct(searchVec);
                                    if (dotLineA < cosCone) continue;
                                    double angleA = Math.acos(Math.max(-1.0, Math.min(1.0, dotLineA)));

                                    // 5. 评估 B 端夹角：连线方向与 B 端“迎向”切线方向的夹角
                                    Vec3d candForward = forwardVector(cand);
                                    Vec3d candFacing;
                                    if (i == 0) {
                                        candFacing = candForward;
                                    } else if (i == lastIdx) {
                                        candFacing = candForward.scale(-1);
                                    } else {
                                        candFacing = (candForward.dotProduct(lineDir) > 0) ? candForward : candForward.scale(-1);
                                    }

                                    double dotLineB = lineDir.dotProduct(candFacing);
                                    if ((i == 0 || i == lastIdx) && dotLineB < cosCone) {
                                        continue;
                                    }
                                    double angleB = Math.acos(Math.max(-1.0, Math.min(1.0, dotLineB)));

                                    // 6. 综合评分
                                    // 姿态偏差（主项）
                                    double angleScore = angleA + angleB;
                                    // 距离归一化，乘以微小系数，作为平局决胜项（0.05 rad ≈ 2.87°）
                                    double distNorm = d / maxDist;
                                    double finalScore = angleScore + distNorm * 0.05;

                                    if (finalScore < bestScore) {
                                        bestScore = finalScore;
                                        best = new TrackNode(seg, i, world);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    // 3D 前向：由 yaw + pitch 决定，roll 不参与
    // 3D 前向：VecYPR 的 yaw 是 IR 约定，先转成 MC yaw；pitch 不参与 roll
    private static Vec3d forwardVector(VecYPR p) {
        // IR yaw -> MC yaw（和 TrackSnapUtil 里的 yawHead 转换一致）
        float mcYaw = ((540 - p.getYaw()) % 360 + 540) % 360;
        double yawRad = Math.toRadians(mcYaw);
        double pitchRad = Math.toRadians(p.getPitch());
        double cp = Math.cos(pitchRad);
        return new Vec3d(
                -Math.sin(yawRad) * cp,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cp
        );
    }
}
