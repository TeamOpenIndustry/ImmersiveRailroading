package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.thirdparty.trackapi.IRPathingData;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OffWorldMovementTrack {
    private static final int MAX_ITERATIONS = 64;
    private static final double EPS = 1e-6;

    public static void getNextPosition(IRPathingData current, Vec3d motion, double gauge, World world) {
        WorldData data = WorldData.get(world);
        if (data == null) return;

        double motionDist = motion.length();
        if (motionDist < EPS) {
            if(current.getTopoEdge() != null) {
                TrackEdge edge = current.getTopoEdge();
                if(edge.edgeType == TrackEdge.EdgeType.NORMAL) {
                    Vec3i trackBlockPos = TrackRegionUtil.toBlockPos(edge.from.regionPos(), edge.from.regionBlockPos());
                    TrackMultiGeometrySegment segment = WorldData.get(world).getTrackBlockEnsured(trackBlockPos);
                    if(segment != null && segment.type == TrackItems.TURNTABLE) {
                        Vec3d next = positionOnEdge(edge, current.getTopoT(), world);
                        current.advanceTo(next);//special
                    }
                }
            } else {
                initCurrent(current, world, gauge);
            }
            return;
        }

        if (current.getTopoEdge() == null) {
            if (!initCurrent(current, world, gauge)) return;
        }

        double[] posOut = new double[4];
        Set<TrackEdge> zeroVisited = new HashSet<>();

        for (int iter = 0; iter < MAX_ITERATIONS && motionDist > EPS; iter++) {
            TrackEdge edge = current.getTopoEdge();
            if (edge == null) return;

            TrackNode from = data.nodes.get(edge.from);
            TrackNode to = data.nodes.get(edge.to);
            if (from == null || to == null) return;

            double edgeDist = edgeDistance(edge, from, to, world);

            if (edgeDist < EPS) {
                // 零边不消耗 motionDist，必须有本次调用的环路保护，
                // 否则零边链一旦绕回来会跑满 MAX_ITERATIONS。
                if (!zeroVisited.add(edge)) return;

                TrackEdge next = pickNextEdgeFrom(data, edge, edge.to, motion, world);
                if (next == null) {
                    next = pickNextEdgeFrom(data, edge, edge.from, motion, world);
                }
                if (next == null) return;

                TrackNode nFrom = data.nodes.get(next.from);
                TrackNode nTo = data.nodes.get(next.to);
                if (nFrom == null || nTo == null) return;

                double startT = next.from.equals(edge.to) || next.from.equals(edge.from) ? 0.0 : 1.0;
                positionAt(next, nFrom, nTo, startT, world, posOut);

                double nextDist = edgeDistance(next, nFrom, nTo, world);
                double startRoll = posOut[3];
                if (nextDist >= EPS) {
                    Vec3d nextDir = edgeDirection(next, nFrom, nTo, world);
                    if (nextDir != null) {
                        double nextDot = nextDir.x * motion.x + nextDir.y * motion.y + nextDir.z * motion.z;
                        startRoll = (nextDot >= 0) ? posOut[3] : -posOut[3];
                    }
                }

                current.advanceTo(next, startT, startRoll);
                current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), startRoll);
                continue;
            }

            Vec3d edgeDir = edgeDirection(edge, from, to, world);
            if (edgeDir == null) return;
            double dot = edgeDir.x * motion.x + edgeDir.y * motion.y + edgeDir.z * motion.z;
            boolean towardTo = dot >= 0;
            double tTarget = towardTo ? 1.0 : 0.0;

            double t = current.getTopoT();
            if (t < 0) t = 0;
            if (t > 1) t = 1;

            double distToEnd = edgeDist * Math.abs(tTarget - t);

            if (motionDist < distToEnd) {
                double tDelta = motionDist / edgeDist;
                double newT = towardTo ? t + tDelta : t - tDelta;
                if (newT < 0) newT = 0;
                if (newT > 1) newT = 1;
                positionAt(edge, from, to, newT, world, posOut);
                double roll = towardTo ? posOut[3] : -posOut[3];
                current.advanceTo(edge, newT, roll);
                current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), roll);
                return;
            }

            motionDist -= distToEnd;
            positionAt(edge, from, to, tTarget, world, posOut);
            double endRoll = towardTo ? posOut[3] : -posOut[3];
            current.advanceTo(edge, tTarget, endRoll);
            current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), endRoll);

            TrackNodeId endNodeId = towardTo ? edge.to : edge.from;
            TrackEdge next = pickNextEdgeFrom(data, edge, endNodeId, motion, world);
            if (next == null) return;

            TrackNode nFrom = data.nodes.get(next.from);
            TrackNode nTo = data.nodes.get(next.to);
            if (nFrom == null || nTo == null) return;

            double startT = next.from.equals(endNodeId) ? 0.0 : 1.0;
            positionAt(next, nFrom, nTo, startT, world, posOut);

            double nextDist = edgeDistance(next, nFrom, nTo, world);
            double startRoll = posOut[3];
            if (nextDist >= EPS) {
                Vec3d nextDir = edgeDirection(next, nFrom, nTo, world);
                if (nextDir != null) {
                    double nextDot = nextDir.x * motion.x + nextDir.y * motion.y + nextDir.z * motion.z;
                    startRoll = (nextDot >= 0) ? posOut[3] : -posOut[3];
                }
            }

            current.advanceTo(next, startT, startRoll);
            current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), startRoll);
        }
    }

    private static double edgeDistance(TrackEdge edge, TrackNode from, TrackNode to, World world) {
        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            double dx = to.point.x - from.point.x;
            double dy = to.point.y - from.point.y;
            double dz = to.point.z - from.point.z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }

        List<VecYPR> points = from.trackSingleGeometrySegment.pointsCache;
        int fi = from.index;
        int ti = to.index;
        if (fi == ti) return 0;
        int lo = Math.max(0, Math.min(fi, ti));
        int hi = Math.min(points.size() - 1, Math.max(fi, ti));
        double d = 0;
        for (int i = lo; i < hi; i++) {
            VecYPR a = points.get(i);
            VecYPR b = points.get(i + 1);
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double dz = b.z - a.z;
            d += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return d;
    }

    private static void positionAt(TrackEdge edge, TrackNode from, TrackNode to, double t, World world, double[] out) {
        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            out[0] = from.point.x * (1 - t) + to.point.x * t;
            out[1] = from.point.y * (1 - t) + to.point.y * t;
            out[2] = from.point.z * (1 - t) + to.point.z * t;

            double fromRollSign = (from.index == 0) ? -1.0 : 1.0;
            double toRollSign = (to.index == 0) ? 1.0 : -1.0;
            double fromRoll = from.point.getRoll() * fromRollSign;
            double toRoll = to.point.getRoll() * toRollSign;
            out[3] = fromRoll * (1 - t) + toRoll * t;
            return;
        }

        List<VecYPR> points = from.trackSingleGeometrySegment.pointsCache;
        int lastIdx = points.size() - 1;
        double actualIdx = from.index + (to.index - from.index) * t;
        int i = (int) Math.floor(actualIdx);
        double frac = actualIdx - i;
        if (i < 0) { i = 0; frac = 0; }
        if (i >= lastIdx) { i = lastIdx; frac = 0; }

        Vec3d offset = singleOffset(from, world);
        VecYPR a = points.get(i);
        double rawRoll;
        if (frac < EPS || i >= lastIdx) {
            out[0] = a.x + offset.x;
            out[1] = a.y + offset.y;
            out[2] = a.z + offset.z;
            rawRoll = a.getRoll();
        } else {
            VecYPR b = points.get(i + 1);
            out[0] = a.x + (b.x - a.x) * frac + offset.x;
            out[1] = a.y + (b.y - a.y) * frac + offset.y;
            out[2] = a.z + (b.z - a.z) * frac + offset.z;
            rawRoll = a.getRoll() + (b.getRoll() - a.getRoll()) * frac;
        }

        if (to.index < from.index) {
            rawRoll = -rawRoll;
        }
        out[3] = rawRoll;
    }

    private static Vec3d edgeDirection(TrackEdge edge, TrackNode from, TrackNode to, World world) {
        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            Vec3d dir = to.point.subtract(from.point);
            if (dir.length() >= TrackNode.GAP_MIN_DIST + EPS) {
                return dir.normalize();
            }
            // 零长度 GAP 没有弦长。不能用 (0,0,1) 伪造方向，也不能用 absPos 相减：
            // 前者会让 +Z 运动把零边当成完全顺向，后者对同一个方块恒为零向量。
            // 正确来源是端点所在 segment 的相邻采样点。
            return zeroLengthEdgeDirection(from, to, world);
        }

        List<VecYPR> points = from.trackSingleGeometrySegment.pointsCache;
        int fi = from.index;
        int ti = to.index;
        if (fi == ti) return zeroLengthEdgeDirection(from, to, world);
        int dir = (ti > fi) ? 1 : -1;
        int adj = fi + dir;
        if (fi < 0 || fi >= points.size() || adj < 0 || adj >= points.size()) {
            return zeroLengthEdgeDirection(from, to, world);
        }
        Vec3d tangent = points.get(adj).subtract(points.get(fi)).normalize();
        if (tangent.length() < EPS) return zeroLengthEdgeDirection(from, to, world);
        return tangent;
    }

    /**
     * 端点重合的边没有弦长，方向取“从节点进入自身 segment”的切线。
     * 普通接缝用 to 端的外向切线（也就是车到达之后的继续方向）；
     * 道岔 tie GAP（hub <-> turnStart）两侧都是 path 起点，必须用弯轨切线，
     * 且 turnStart -> hub 时取反，不能用 hub 的直轨切线代替。
     */
    private static Vec3d zeroLengthEdgeDirection(TrackNode from, TrackNode to, World world) {
        WorldData data = WorldData.get(world);
        if (data != null) {
            WorldData.SwitchInfo si = data.switchByHub.get(from.id);
            if (si == null) si = data.switchByHub.get(to.id);
            if (si != null) {
                TrackNode turnStart = null;
                if (si.turnStarts.contains(from.id)) {
                    turnStart = from;
                } else if (si.turnStarts.contains(to.id)) {
                    turnStart = to;
                }
                if (turnStart != null) {
                    Vec3d turnDir = tangentAwayFromNode(turnStart, null);
                    if (turnDir != null) {
                        return from.id.equals(turnStart.id) ? turnDir.scale(-1) : turnDir;
                    }
                }
            }
        }

        Vec3d sourceIncoming = tangentAwayFromNode(from, null);
        if (sourceIncoming != null) sourceIncoming = sourceIncoming.scale(-1);

        Vec3d dir = tangentAwayFromNode(to, sourceIncoming);
        if (dir != null) return dir;

        return sourceIncoming;
    }

    /**
     * 从 node 指向自己 segment 内部的单位切线。
     * index 0 用 points[1]-points[0]；index last 用 points[last-1]-points[last]。
     * 内部点有两个方向，preferred 非空时取和它更同向的一侧。
     */
    private static Vec3d tangentAwayFromNode(TrackNode node, Vec3d preferred) {
        if (node == null) return null;
        List<VecYPR> points = node.trackSingleGeometrySegment.pointsCache;
        if (points == null || points.size() < 2) return null;

        int i = node.index;
        if (i < 0 || i >= points.size()) return null;

        if (i == 0) {
            return tangent(points.get(0), points.get(1));
        }
        if (i == points.size() - 1) {
            return tangent(points.get(points.size() - 1), points.get(points.size() - 2));
        }

        Vec3d forward = tangent(points.get(i), points.get(i + 1));
        Vec3d backward = tangent(points.get(i), points.get(i - 1));
        if (forward == null) return backward;
        if (backward == null) return forward;
        if (preferred == null) return forward;
        return forward.dotProduct(preferred) >= backward.dotProduct(preferred) ? forward : backward;
    }

    private static Vec3d tangent(VecYPR from, VecYPR to) {
        Vec3d dir = to.subtract(from).normalize();
        return dir.length() < EPS ? null : dir;
    }

    private static TrackEdge pickNextEdgeFrom(WorldData data, TrackEdge incoming, TrackNodeId endNodeId,
                                              Vec3d motion, World world) {
        WorldData.SwitchInfo si = data.switchByHub.get(endNodeId);
        if (si != null) return pickFromHub(data, incoming, endNodeId, si);

        List<TrackEdge> candidates = data.outgoing.get(endNodeId);
        if (candidates == null || candidates.isEmpty()) return null;

        TrackEdge best = null;
        double bestCos = -1 - EPS;

        for (TrackEdge e : candidates) {
            if (e.equals(incoming)) continue;

            TrackNode eFrom = data.nodes.get(e.from);
            TrackNode eTo = data.nodes.get(e.to);
            if (eFrom == null || eTo == null) continue;

            // 零长度候选也直接由自身端点切线给出方向，不再递归跳到 inner。
            // 递归会让 A->B 和 B->A 两条零边互相引用，最终掉回 +Z 回退。
            Vec3d dir = edgeDirection(e, eFrom, eTo, world);
            if (dir == null) continue;

            double cos = dir.x * motion.x + dir.y * motion.y + dir.z * motion.z;
            if (cos > bestCos) {
                bestCos = cos;
                best = e;
            }
        }
        return best;
    }

    /*
     * 岔心逻辑：
     *  - 车从分支来（straightEnd 或 turnStart）→ 直接出外部（不判 switchState，车已经在分支上）
     *  - 车从外部 GAP 来 → 按 switchState 选分支
     */
    private static TrackEdge pickFromHub(WorldData data, TrackEdge incoming, TrackNodeId hubId,
                                         WorldData.SwitchInfo si) {
        TrackNodeId cameFrom = incoming.to.equals(hubId) ? incoming.from : incoming.to;

        boolean fromBranch = cameFrom.equals(si.straightEnd) || si.turnStarts.contains(cameFrom);
        if (fromBranch) {
            return pickExternalFromHub(data, hubId, incoming, si);
        }

        SwitchState state = si.getState(data);
        TrackNodeId target;
        if (state == SwitchState.STRAIGHT || state == SwitchState.NONE) {
            target = si.straightEnd;
        } else {
            int idx = (state == SwitchState.TURN)
                    ? 0
                    : state.ordinal() - SwitchState.TURN.ordinal();
            if (idx < 0 || idx >= si.turnStarts.size()) return null;
            target = si.turnStarts.get(idx);
        }

        List<TrackEdge> cands = data.outgoing.get(hubId);
        if (cands == null) return null;
        for (TrackEdge e : cands) {
            if (e.equals(incoming)) continue;
            if (e.to.equals(target)) return e;
        }
        return null;
    }

    /* 车从分支来出外部：hub 的 outgoing 里，跳过 NORMAL 和 tie GAP，返回外部 GAP */
    private static TrackEdge pickExternalFromHub(WorldData data, TrackNodeId hubId,
                                                 TrackEdge incoming, WorldData.SwitchInfo si) {
        List<TrackEdge> cands = data.outgoing.get(hubId);
        if (cands == null) return null;
        for (TrackEdge e : cands) {
            if (e.equals(incoming)) continue;
            if (e.edgeType != TrackEdge.EdgeType.GAP) continue;
            if (si.turnStarts.contains(e.to)) continue;
            return e;
        }
        return null;
    }

    public static boolean initCurrent(IRPathingData current, World world, double gauge) {
        WorldData data = WorldData.get(world);
        if (data == null) return false;

        Vec3d pos = current.getUMCPos();
        double maxRadius = Math.max(gauge * 2, 2);
        double maxRadiusSq = maxRadius * maxRadius;

        int hori = Math.max((int)(gauge * 2), 1);
        int vert = 1;

        Vec3i posBlock = new Vec3i(pos);
        Set<TrackEdge> candidates = new HashSet<>();

        Set<Long> regionsToCheck = new HashSet<>();
        for (int x = -hori; x <= hori; x++) {
            for (int y = -vert; y <= vert; y++) {
                for (int z = -hori; z <= hori; z++) {
                    Vec3i scan = posBlock.add(x, y, z);
                    regionsToCheck.add(TrackRegionUtil.vecToRegion(scan));

                    List<Vec3i> parents = data.getParentsAt(scan);
                    if (parents != null) {
                        for (Vec3i parentAbs : parents) {
                            regionsToCheck.add(TrackRegionUtil.vecToRegion(parentAbs));
                        }
                    }
                }
            }
        }

        for (long r : regionsToCheck) {
            Set<TrackEdge> list = data.edgesByRegion.get(r);
            if (list != null) candidates.addAll(list);
        }

        TrackEdge bestEdge = null;
        double bestT = 0;
        double bestDistSq = Double.MAX_VALUE;

        for (TrackEdge edge : candidates) {
            TrackNode from = data.nodes.get(edge.from);
            TrackNode to = data.nodes.get(edge.to);
            if (from == null || to == null) continue;

            double t = projectOntoEdge(edge, pos, world);
            if (t < 0) continue;

            double[] out = new double[4];
            positionAt(edge, from, to, t, world, out);
            double ddx = out[0] - pos.x;
            double ddy = out[1] - pos.y;
            double ddz = out[2] - pos.z;
            double d2 = ddx * ddx + ddy * ddy + ddz * ddz;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                bestEdge = edge;
                bestT = t;
            }
        }

        if (bestEdge == null) return false;
        if (bestDistSq > maxRadiusSq) return false;

        TrackNode from = data.nodes.get(bestEdge.from);
        TrackNode to = data.nodes.get(bestEdge.to);
        if (from == null || to == null) return false;

        double[] out = new double[4];
        positionAt(bestEdge, from, to, bestT, world, out);
        current.advanceTo(bestEdge, bestT, out[3]);
        current.advanceTo(new Vec3d(out[0], out[1], out[2]), out[3]);
        return true;
    }

    public static double projectOntoEdge(TrackEdge edge, Vec3d pos, World world) {
        WorldData data = WorldData.get(world);
        if (data == null) return -1;
        TrackNode from = data.nodes.get(edge.from);
        TrackNode to = data.nodes.get(edge.to);
        if (from == null || to == null) return -1;

        double maxDist = Math.max(1.0, from.trackSingleGeometrySegment.gauge.value() * 2);
        double maxDistSq = maxDist * maxDist;

        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            double ay = from.point.y;
            double by = to.point.y;
            double ax = to.point.x - from.point.x;
            double ayy = by - ay;
            double az = to.point.z - from.point.z;
            double abLen2 = ax * ax + ayy * ayy + az * az;
            if (abLen2 < EPS) {
                // Zero-length GAP: both ends are the same point, so any t is positionally
                // equivalent. Keep the topology if pos is close enough instead of forcing
                // the caller back to initCurrent.
                return from.point.distanceToSquared(pos) <= maxDistSq ? 0 : -1;
            }
            double t = ((pos.x - from.point.x) * ax + (pos.y - ay) * ayy + (pos.z - from.point.z) * az) / abLen2;
            t = Math.max(0, Math.min(1, t));
            double cx = from.point.x + ax * t - pos.x;
            double cy = from.point.y + ayy * t - pos.y;
            double cz = from.point.z + az * t - pos.z;
            if (cx * cx + cy * cy + cz * cz > maxDistSq) return -1;
            return t;
        }

        List<VecYPR> points = from.trackSingleGeometrySegment.pointsCache;
        Vec3d offset = singleOffset(from, world);
        int fi = from.index;
        int ti = to.index;
        if (fi == ti) return -1;

        int lastIdx = points.size() - 1;
        int lo = Math.max(0, Math.min(fi, ti));
        int hi = Math.min(lastIdx, Math.max(fi, ti));
        if (lo >= hi) return -1;

        double bestDistSq = Double.MAX_VALUE;
        int bestSeg = -1;
        double bestFrac = 0;

        for (int i = lo; i < hi; i++) {
            VecYPR pa = points.get(i);
            VecYPR pb = points.get(i + 1);
            double ax = pa.x + offset.x, ay = pa.y + offset.y, az = pa.z + offset.z;
            double bx = pb.x + offset.x, by = pb.y + offset.y, bz = pb.z + offset.z;
            double dx = bx - ax, dy = by - ay, dz = bz - az;
            double segLen2 = dx * dx + dy * dy + dz * dz;
            if (segLen2 < EPS) continue;
            double tt = ((pos.x - ax) * dx + (pos.y - ay) * dy + (pos.z - az) * dz) / segLen2;
            if (tt < 0) tt = 0;
            if (tt > 1) tt = 1;
            double cx = ax + dx * tt - pos.x;
            double cy = ay + dy * tt - pos.y;
            double cz = az + dz * tt - pos.z;
            double d2 = cx * cx + cy * cy + cz * cz;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                bestSeg = i;
                bestFrac = tt;
            }
        }

        if (bestSeg < 0 || bestDistSq > maxDistSq) return -1;

        double actualIdx = bestSeg + bestFrac;
        int denom = ti - fi;
        double t = (actualIdx - fi) / denom;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    public static Vec3d positionOnEdge(TrackEdge edge, double t, World world) {
        WorldData data = WorldData.get(world);
        if (data == null) return Vec3d.ZERO;
        TrackNode from = data.nodes.get(edge.from);
        TrackNode to = data.nodes.get(edge.to);
        if (from == null || to == null) return Vec3d.ZERO;
        double[] out = new double[4];
        positionAt(edge, from, to, t, world, out);
        return new Vec3d(out[0], out[1], out[2]);
    }

    private static Vec3d singleOffset(TrackNode node, World world) {
        TrackSingleGeometrySegment single = node.trackSingleGeometrySegment;
        Vec3i parentPos = single.getAbsPos();
        RailInfo info = single.getAndUpdateBuilder(world).info;
        Vec3d pp = info.placementInfo.placementPosition;
        double h = info.getTrackHeight() * single.gauge.scale();
        return new Vec3d(parentPos.x + pp.x, parentPos.y + pp.y + h, parentPos.z + pp.z);
    }
}
