package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

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
        Vec3i pos = segment.getBlockPos();
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

        final double maxAngleRad = Math.toRadians(30);
        final double cosCone = Math.cos(maxAngleRad);

        Vec3d forwardVec = forwardVector(this.point);
        Vec3d coneVec = forward ? forwardVec : forwardVec.scale(-1);

        Vec3i selfBlock = TrackRegionUtil.toBlockPos(regionPos, regionBlockPos);

        for (int x = -hori; x <= hori; x++) {
            for (int y = -vert; y <= vert; y++) {
                for (int z = -hori; z <= hori; z++) {
                    Vec3i scan = new Vec3i(this.point).add(x, y, z);
                    List<Vec3i> parents = data.getParentsAt(scan);
                    if (parents == null) continue;

                    for (Vec3i parentPos : parents) {
                        if (parentPos.equals(selfBlock)) continue;

                        TrackMultiGeometrySegment trackBlock = data.getTrackBlock(parentPos);
                        if (trackBlock == null) continue;

                        // 改：扫所有 path，不再只扫 getFirst()
                        for (Map<Gauge, TrackSingleGeometrySegment> branch : trackBlock.paths) {
                            for (Map.Entry<Gauge, TrackSingleGeometrySegment> single : branch.entrySet()) {
                                TrackSingleGeometrySegment seg = single.getValue();
                                List<VecYPR> points = seg.pointsCache;
                                double maxDist = seg.gauge.value();
                                for (int i = 0; i < points.size(); i++) {
                                    // 跳过 turnStart（岔尖由 hub↔turnStart GAP 专门处理）
                                    if (seg.switchIdex > 0 && i == 0) continue;

                                    VecYPR cand = points.get(i)
                                            .add(new Vec3d(parentPos))
                                            .add(seg.getAndUpdateBuilder(world).info.placementInfo.placementPosition);

                                    double d = cand.distanceTo(this.point);
                                    if (d >= maxDist) continue;

                                    if (d < 1e-3) {
                                        return new TrackNode(seg, i, world);
                                    }

                                    Vec3d offset = new Vec3d(
                                            cand.x - this.point.x,
                                            cand.y - this.point.y,
                                            cand.z - this.point.z
                                    );
                                    double dist = offset.length();
                                    if (dist > 1e-4) {
                                        Vec3d dir = offset.scale(1.0 / dist);
                                        if (dir.dotProduct(coneVec) < cosCone) continue;
                                    }

                                    Vec3d candForward = forwardVector(cand);
                                    double dotFF = forwardVec.dotProduct(candForward);
                                    double absDot = Math.max(0.0, Math.min(1.0, Math.abs(dotFF)));
                                    double angleRad = Math.acos(absDot);
                                    if (angleRad > maxAngleRad) continue;

                                    double distNorm  = d / maxDist;
                                    double angleNorm = angleRad / maxAngleRad;
                                    double score = 0.7 * distNorm + 0.3 * angleNorm;

                                    if (score < bestScore) {
                                        bestScore = score;
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
