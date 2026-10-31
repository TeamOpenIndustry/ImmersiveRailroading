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
                    Vec3i trackBlockPos = TrackRegionUtil.toBlockPos(edge.from.regionPos, edge.from.regionBlockPos);
                    TrackMultiGeometrySegment segment = WorldData.get(world).getTrackBlock(trackBlockPos);
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

        for (int iter = 0; iter < MAX_ITERATIONS && motionDist > EPS; iter++) {
            TrackEdge edge = current.getTopoEdge();
            if (edge == null) return;

            TrackNode from = data.nodes.get(edge.from);
            TrackNode to = data.nodes.get(edge.to);
            if (from == null || to == null) return;

            double edgeDist = edgeDistance(edge, from, to, world);

            // 零长度边（几何重合的 GAP）：不消耗 motionDist，直接切到下一条边
            if (edgeDist < EPS) {
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
                double startRoll;
                if (nextDist < EPS) {
                    startRoll = posOut[3];
                } else {
                    Vec3d nextDir = edgeDirection(next, nFrom, nTo, world);
                    double nextDot = nextDir.x * motion.x + nextDir.y * motion.y + nextDir.z * motion.z;
                    startRoll = (nextDot >= 0) ? posOut[3] : -posOut[3];
                }

                current.advanceTo(next, startT, startRoll);
                current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), startRoll);
                continue;
            }

            // 由 motion 与边方向的点积决定 t 增还是减
            Vec3d edgeDir = edgeDirection(edge, from, to, world);
            double dot = edgeDir.x * motion.x + edgeDir.y * motion.y + edgeDir.z * motion.z;
            boolean towardTo = dot >= 0;
            double tTarget = towardTo ? 1.0 : 0.0;

            double t = current.getTopoT();
            if (t < 0) t = 0;
            if (t > 1) t = 1;

            double distToEnd = edgeDist * Math.abs(tTarget - t);

            // 走不完当前边：直接推进 t 后退出
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

            // 走到边尾
            motionDist -= distToEnd;
            positionAt(edge, from, to, tTarget, world, posOut);
            double endRoll = towardTo ? posOut[3] : -posOut[3];
            current.advanceTo(edge, tTarget, endRoll);
            current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), endRoll);

            // 从到达端挑下一条边
            TrackNodeId endNodeId = towardTo ? edge.to : edge.from;
            TrackEdge next = pickNextEdgeFrom(data, edge, endNodeId, motion, world);
            if (next == null) return;

            TrackNode nFrom = data.nodes.get(next.from);
            TrackNode nTo = data.nodes.get(next.to);
            if (nFrom == null || nTo == null) return;

            double startT = next.from.equals(endNodeId) ? 0.0 : 1.0;
            positionAt(next, nFrom, nTo, startT, world, posOut);

            double nextDist = edgeDistance(next, nFrom, nTo, world);
            double startRoll;
            if (nextDist < EPS) {
                startRoll = posOut[3];
            } else {
                Vec3d nextDir = edgeDirection(next, nFrom, nTo, world);
                double nextDot = nextDir.x * motion.x + nextDir.y * motion.y + nextDir.z * motion.z;
                startRoll = (nextDot >= 0) ? posOut[3] : -posOut[3];
            }

            current.advanceTo(next, startT, startRoll);
            current.advanceTo(new Vec3d(posOut[0], posOut[1], posOut[2]), startRoll);
        }
    }

    // ==================== 边几何 ====================

    private static double edgeDistance(TrackEdge edge, TrackNode from, TrackNode to, World world) {
        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            double hFrom = trackHeight(from, world);
            double hTo = trackHeight(to, world);
            double dx = to.point.x - from.point.x;
            double dy = (to.point.y + hTo) - (from.point.y + hFrom);
            double dz = to.point.z - from.point.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
            double hFrom = trackHeight(from, world);
            double hTo = trackHeight(to, world);
            double h = hFrom * (1 - t) + hTo * t;

            out[0] = from.point.x * (1 - t) + to.point.x * t;
            out[1] = from.point.y * (1 - t) + to.point.y * t + h;
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
            double hFrom = trackHeight(from, world);
            double hTo = trackHeight(to, world);
            double dx = to.point.x - from.point.x;
            double dy = (to.point.y + hTo) - (from.point.y + hFrom);
            double dz = to.point.z - from.point.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < EPS) return new Vec3d(0, 0, 1);
            return new Vec3d(dx / len, dy / len, dz / len);
        }

        List<VecYPR> points = from.trackSingleGeometrySegment.pointsCache;
        int fi = from.index;
        int ti = to.index;
        if (fi == ti) return new Vec3d(0, 0, 1);
        int dir = (ti > fi) ? 1 : -1;
        int adj = fi + dir;
        if (fi < 0 || fi >= points.size() || adj < 0 || adj >= points.size()) {
            return new Vec3d(0, 0, 1);
        }
        VecYPR a = points.get(fi);
        VecYPR b = points.get(adj);
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < EPS) return new Vec3d(0, 0, 1);
        return new Vec3d(dx / len, dy / len, dz / len);
    }

    // ==================== 选下一条边 ====================

    private static TrackEdge pickNextEdgeFrom(WorldData data, TrackEdge incoming, TrackNodeId endNodeId,
                                              Vec3d motion, World world) {
        return pickNextEdgeFrom(data, incoming, endNodeId, motion, world, 0);
    }

    private static TrackEdge pickNextEdgeFrom(WorldData data, TrackEdge incoming, TrackNodeId endNodeId,
                                              Vec3d motion, World world, int depth) {
        if (depth > 8) return null;

        WorldData.SwitchInfo si = data.switchByHub.get(endNodeId);
        if (si != null) return pickFromHub(data, incoming, si);

        si = data.switchByTurn.get(endNodeId);
        if (si != null) return pickFromTurnStart(data, incoming, endNodeId, si);

        List<TrackEdge> candidates = data.outgoing.get(endNodeId);
        if (candidates == null || candidates.isEmpty()) return null;

        TrackEdge best = null;
        double bestCos = 0.0;

        for (TrackEdge e : candidates) {
            if (e.equals(incoming)) continue;

            TrackNode eFrom = data.nodes.get(e.from);
            TrackNode eTo = data.nodes.get(e.to);
            if (eFrom == null || eTo == null) continue;

            double eDist = edgeDistance(e, eFrom, eTo, world);
            Vec3d dir;
            if (eDist < EPS) {
                TrackEdge inner = pickNextEdgeFrom(data, e, e.to, motion, world, depth + 1);
                if (inner == null) continue;
                TrackNode innerFrom = data.nodes.get(inner.from);
                TrackNode innerTo = data.nodes.get(inner.to);
                if (innerFrom == null || innerTo == null) continue;
                dir = edgeDirection(inner, innerFrom, innerTo, world);
            } else {
                dir = edgeDirection(e, eFrom, eTo, world);
            }

            double cos = dir.x * motion.x + dir.y * motion.y + dir.z * motion.z;
            if (cos > bestCos) {
                bestCos = cos;
                best = e;
            }
        }
        return best;
    }

    private static TrackEdge pickFromHub(WorldData data, TrackEdge incoming, WorldData.SwitchInfo si) {
        List<TrackEdge> cands = data.outgoing.get(si.hub);
        if (cands == null) return null;

        TrackNodeId cameFrom = incoming.to.equals(si.hub) ? incoming.from : incoming.to;

        if (incoming.edgeType == TrackEdge.EdgeType.NORMAL && cameFrom.equals(si.straightEnd)) {
            return pickExternalGapFromHub(cands, incoming, si);
        }
        if (incoming.edgeType == TrackEdge.EdgeType.GAP) {
            for (WorldData.SwitchInfo.Branch b : si.turns) {
                if (cameFrom.equals(b.start)) {
                    return pickExternalGapFromHub(cands, incoming, si);
                }
            }
        }

        int branchIdx = branchIndexFromState(si.segment.getSwitchState());
        if (branchIdx <= 0) {
            for (TrackEdge e : cands) {
                if (e.equals(incoming)) continue;
                if (e.edgeType == TrackEdge.EdgeType.NORMAL && e.to.equals(si.straightEnd)) return e;
            }
            return null;
        }

        int turnIdx = branchIdx - 1;
        if (turnIdx < 0 || turnIdx >= si.turns.size()) return null;
        TrackNodeId targetStart = si.turns.get(turnIdx).start;

        for (TrackEdge e : cands) {
            if (e.equals(incoming)) continue;
            if (e.edgeType == TrackEdge.EdgeType.GAP && e.to.equals(targetStart)) return e;
        }
        return null;
    }

    private static TrackEdge pickExternalGapFromHub(List<TrackEdge> cands, TrackEdge incoming,
                                                    WorldData.SwitchInfo si) {
        for (TrackEdge e : cands) {
            if (e.equals(incoming)) continue;
            if (e.edgeType != TrackEdge.EdgeType.GAP) continue;

            boolean isTie = false;
            for (WorldData.SwitchInfo.Branch b : si.turns) {
                if (e.to.equals(b.start)) { isTie = true; break; }
            }
            if (isTie) continue;
            return e;
        }
        return null;
    }

    private static TrackEdge pickFromTurnStart(WorldData data, TrackEdge incoming, TrackNodeId turnStartId,
                                               WorldData.SwitchInfo si) {
        List<TrackEdge> cands = data.outgoing.get(turnStartId);
        if (cands == null) return null;

        TrackNodeId cameFrom = incoming.to.equals(turnStartId) ? incoming.from : incoming.to;
        boolean fromTie = incoming.edgeType == TrackEdge.EdgeType.GAP && cameFrom.equals(si.hub);

        if (fromTie) {
            for (TrackEdge e : cands) {
                if (e.equals(incoming)) continue;
                if (e.edgeType == TrackEdge.EdgeType.NORMAL) return e;
            }
        } else {
            for (TrackEdge e : cands) {
                if (e.equals(incoming)) continue;
                if (e.edgeType == TrackEdge.EdgeType.GAP && e.to.equals(si.hub)) return e;
            }
        }
        return null;
    }

    private static int branchIndexFromState(int switchStateOrdinal) {
        if (switchStateOrdinal == SwitchState.NONE.ordinal()
                || switchStateOrdinal == SwitchState.STRAIGHT.ordinal()) return 0;
        if (switchStateOrdinal == SwitchState.TURN.ordinal()) return 1;
        return switchStateOrdinal;
    }

    // ==================== 初始化 ====================

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

        // 收集 pos 附近所有方块涉及的 region
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

    // ==================== 投影 ====================

    public static double projectOntoEdge(TrackEdge edge, Vec3d pos, World world) {
        WorldData data = WorldData.get(world);
        if (data == null) return -1;
        TrackNode from = data.nodes.get(edge.from);
        TrackNode to = data.nodes.get(edge.to);
        if (from == null || to == null) return -1;

        double maxDist = Math.max(1.0, from.trackSingleGeometrySegment.gauge.value() * 2);
        double maxDistSq = maxDist * maxDist;

        if (edge.edgeType == TrackEdge.EdgeType.GAP) {
            double hFrom = trackHeight(from, world);
            double hTo = trackHeight(to, world);
            double ay = from.point.y + hFrom;
            double by = to.point.y + hTo;
            double ax = to.point.x - from.point.x;
            double ayy = by - ay;
            double az = to.point.z - from.point.z;
            double abLen2 = ax * ax + ayy * ayy + az * az;
            if (abLen2 < EPS) return -1;
            double t = ((pos.x - from.point.x) * ax + (pos.y - ay) * ayy + (pos.z - from.point.z) * az) / abLen2;
            t = Math.max(0, Math.min(1, t));
            double cx = from.point.x + ax * t - pos.x;
            double cy = ay + ayy * t - pos.y;
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

    // ==================== 工具 ====================

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

    private static double trackHeight(TrackNode node, World world) {
        TrackSingleGeometrySegment single = node.trackSingleGeometrySegment;
        RailInfo info = single.getAndUpdateBuilder(world).info;
        return info.getTrackHeight() * single.gauge.scale();
    }
}