package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.net.TrackRegionPacket;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static cam72cam.immersiverailroading.track.graph.TrackRegionUtil.vecToRegion;

public class WorldData {
    private final World world;
    private final File directory;
    public final Map<Long, TrackRegion> regions;//todo 客户端如何申请获取某处的region，另外现在还没做unload region，做了会需要处理很多问题

    private final static Map<World, WorldData> LOADED = new HashMap<>();

    // 拓扑层 todo 这三个东西虽然还不完全确定，不过应该也大差不差，不会少于这些
    public final Map<TrackNodeId, TrackNode> nodes = new HashMap<>();
    final Map<TrackNodeId, List<TrackEdge>> outgoing = new HashMap<>();// 为了更新node时能及时更新相关边
    public final Set<TrackEdge> edges = new HashSet<>();

    //todo set/removeTrackBlock加拓扑构建，需注意每次要检查trackBlock的gag以及拓展一定范围的区域能接触到gag的trackBlock也需要更新，保证万无一失

    private WorldData(World world, File worldDirectory) {
        this.world = world;
        this.directory = worldDirectory;
        this.regions = new HashMap<>();

        if (worldDirectory != null && worldDirectory.exists()) {
            File[] files = worldDirectory.listFiles();
            if (files != null) {
                Arrays.stream(files).parallel().forEach(file -> {
                    String name = file.getName();
                    if (!name.endsWith(".irr")) {
                        return;
                    }
                    String baseName = name.substring(0, name.length() - 4);
                    String[] parts = baseName.split("\\.");
                    if (parts.length != 3 || !parts[0].equals("r")) {
                        return;
                    }
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        long id = ((long) x << 32) | (z & 0xFFFFFFFFL);
                        TrackRegion region = new TrackRegion(id, TrackRegionUtil.readBuffer(file), world);
                        synchronized (regions) {
                            regions.put(id, region);
                        }
                    } catch (IOException | NumberFormatException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    public static WorldData get(World world) {
        return LOADED.get(world);
    }

    public static WorldData getOrCreate(World world) {
        // client only
        return LOADED.computeIfAbsent(world, w -> new WorldData(w, null));
    }

    private File regionFile(long region) {
        int x = TrackRegionUtil.regionX(region);
        int z = TrackRegionUtil.regionZ(region);
        return new File(directory, String.format("r.%d.%d.irr", x, z));
    }

    protected TrackRegion getRegion(Vec3i pos, boolean create) {
        long id = vecToRegion(pos);
        return getRegionById(id, create);
    }

    private TrackRegion getRegionById(long id, boolean create) {
        synchronized (regions) {
            TrackRegion region = regions.get(id);
            if (region == null && create) {
                region = new TrackRegion(id);
                regions.put(id, region);
            }
            return region;
        }
    }

    public TrackMultiGeometrySegment getTrackBlock(Vec3i pos) {
        TrackRegion region = getRegion(pos, false);
        if (region != null) {
            return region.getTrackBlock(pos);
        }
        return null;
    }

    public void setTrackBlock(Vec3i pos, TrackMultiGeometrySegment block) {
        long regionId = vecToRegion(pos);
        TrackRegion region = getRegionById(regionId, true);

        // 同位置有旧的多几何段，先清它的拓扑
        if (region.getTrackBlock(pos) != null) {
            removeTopologyFor(regionId, TrackRegionUtil.toRegionBlockPos(pos));
        }

        region.setTrackBlock(pos, block);

        buildTopologyFor(pos, block);
        researchNeighbors(pos);
    }

    public boolean removeTrackBlock(Vec3i pos) {
        long regionId = vecToRegion(pos);
        TrackRegion region = getRegionById(regionId, false);
        if (region == null) {
            return false;
        }
        if (!region.removeTrackBlock(pos)) {
            return false;
        }

        removeTopologyFor(regionId, TrackRegionUtil.toRegionBlockPos(pos));
        return true;
    }

    public Collection<TrackRegion> getRegions() {
        return regions.values();
    }

    private void saveInternal() {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException(String.format(
                        "Unable to create ImmersiveRailroading data directory %s!", directory));
            }
        }
        if (!directory.isDirectory()) {
            throw new RuntimeException(String.format(
                    "Expected ImmersiveRailroading data directory %s is not a directory!", directory));
        }

        synchronized (regions) {
            Iterator<Map.Entry<Long, TrackRegion>> iterator = regions.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, TrackRegion> entry = iterator.next();
                TrackRegion region = entry.getValue();

                if (!region.needsWriteToDisk) {
                    continue;
                }

                File file = regionFile(entry.getKey());

                if (region.isEmpty()) {
                    // remove from disk and ram
                    if (file.exists() && !file.delete()) {
                        throw new RuntimeException(String.format("Unable to delete empty region file %s!", file));
                    }
                    iterator.remove();
                    continue;
                }

                try {
                    TrackRegionUtil.writeBuffer(file, region.write());
                    region.needsWriteToDisk = false;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void save(World world, File levelDirectory) {
        ImmersiveRailroading.info("Save World %s / %s", levelDirectory.toString(), world.getId());
        long st = System.currentTimeMillis();
        synchronized (LOADED) {
            if (LOADED.containsKey(world)) {
                LOADED.get(world).saveInternal();
            }
        }
        ImmersiveRailroading.info("World %s / %s saved in %sms", levelDirectory.toString(), world.getId(), System.currentTimeMillis() - st);
    }

    public static void load(World world, File levelDirectory) {
        ImmersiveRailroading.info("Load World %s / %s", levelDirectory.toString(), world.getId());
        long st = System.currentTimeMillis();
        synchronized (LOADED) {
            if (LOADED.containsKey(world)) {
                ImmersiveRailroading.warn("World %s / %s already loaded!  This is a bug!", levelDirectory.toString(), world.getId());
            }
            WorldData data = new WorldData(world, new File(levelDirectory, "immersiverailroading" + world.getId()));
            LOADED.put(world, data);

            data.buildAll();
        }
        ImmersiveRailroading.info("World %s / %s loaded in %sms", levelDirectory.toString(), world.getId(), System.currentTimeMillis() - st);
    }

    public static void unload(World world, File levelDirectory) {
        ImmersiveRailroading.info("Unload Level %s / %s", levelDirectory.toString(), world.getId());
        synchronized (LOADED) {
            LOADED.remove(world);//todo 客户端的unload没有覆盖到或者没生效
        }
    }

    public static void tick(World world) {
        WorldData data = get(world);
        if (data != null) {
            data.tick();
        }
    }

    private final Map<Player, Set<Long>> playerRegionMap = new HashMap<>();//todo: sync
    private void tick() {
        if (world.isServer) {
            List<Player> players = world.getEntities(Player.class);

            // cull players who have left this world
            List<Player> removedPlayers = new ArrayList<>(playerRegionMap.keySet());
            removedPlayers.removeAll(players);
            for (Player removedPlayer : removedPlayers) {
                playerRegionMap.remove(removedPlayer);
            }

            // This assumes the client *never* culls the region map
            // Also, this does not work for client pathing super large switches... probably fine for now?
            for (Player player : players) {
                // Should we wait for at least one tick existed?
                Set<Long> sentRegions = playerRegionMap.computeIfAbsent(player, p -> new HashSet<>());
                Set<Long> trackingRegions = new HashSet<>();
                int radius = 16;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        trackingRegions.add(vecToRegion(new Vec3i(player.getPosition()).add(x * 16, 0, z * 16)));
                    }
                }
                for (Long trackingRegion : trackingRegions) {
                    TrackRegion region = regions.get(trackingRegion);

                    if (region == null) {
                        if (sentRegions.contains(trackingRegion)) {
                            sentRegions.remove(trackingRegion);
                            new TrackRegionPacket(world, trackingRegion, null).sendToPlayer(player);
                        }
                        continue;
                    }

                    if (!sentRegions.contains(trackingRegion) || region.dirty) {
                        sentRegions.add(trackingRegion);
                        new TrackRegionPacket(world, trackingRegion, region).sendToPlayer(player);
                    }
                }
            }
            for (TrackRegion region : regions.values()) {
                region.dirty = false;
            }
        }
    }

    //todo 拓扑层草稿代码，简单情况已经初步实现，需要继续完善
    protected void buildTopologyFor(Vec3i absPos, TrackMultiGeometrySegment block) {
        for (int pathIndex = 0; pathIndex < block.paths.size(); pathIndex++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = block.paths.get(pathIndex);
            for (Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                TrackSingleGeometrySegment single = entry.getValue();
                int lastIdx = single.pointsCache.size() - 1;

                if (lastIdx > 0) {
                    // start 节点：自身 yaw 指向 single 内部，离开 single 要用 backward
                    TrackNode startNode = new TrackNode(single, 0, world, absPos);
                    addNode(startNode);
                    searchAndAddGap(single, false);

                    // end 节点：自身 yaw 仍指向 single 内部（从 start 到 end 方向），离开 single 要用 forward
                    TrackNode endNode = new TrackNode(single, lastIdx, world, absPos);
                    addNode(endNode);
                    searchAndAddGap(single, true);

                    // single 内部双向 NORMAL 边
                    addEdge(startNode.id, endNode.id, TrackEdge.EdgeType.NORMAL);
                    addEdge(endNode.id, startNode.id, TrackEdge.EdgeType.NORMAL);
                }
            }
        }
    }

    private void searchAndAddGap(TrackSingleGeometrySegment single, boolean isNear) {//todo 还没加上point只有1个点的支持，之后加，当然调用方也是
        int lastIdx = single.pointsCache.size() - 1;
        if (lastIdx <= 0) return;

        Vec3i absPos = single.getBlockPos();

        TrackNode startNode = new TrackNode(single, 0, world, absPos);
        TrackNode endNode = new TrackNode(single, lastIdx, world, absPos);

        TrackNode node = isNear ? startNode : endNode;
        addNode(node);

        boolean forward = !isNear;
        TrackNode other = node.getConn(world, forward);
        if (other == null || other.id.equals(node.id)) return;

        addNode(other);
        addEdge(node.id, other.id, TrackEdge.EdgeType.GAP);

        int otherLastIdx = other.trackSingleGeometrySegment.pointsCache.size() - 1;
        if (other.index != 0 && other.index != otherLastIdx) {
            Vec3i otherAbsPos = TrackRegionUtil.toBlockPos(other.id.regionPos, other.id.regionBlockPos);
            TrackNode otherStartNode = new TrackNode(other.trackSingleGeometrySegment, 0, world, otherAbsPos);
            TrackNode otherEndNode = new TrackNode(other.trackSingleGeometrySegment, otherLastIdx, world, otherAbsPos);

            Vec3d gapDir = other.point.subtract(node.point).internal();
            double gapLen = gapDir.length();
            if (gapLen < 1e-6) {
                // gap 两端重合，用 node 所在 single 的端点邻点方向代替
                List<VecYPR> nodePoints = node.trackSingleGeometrySegment.pointsCache;
                VecYPR a;
                VecYPR b;
                if (isNear) {
                    a = nodePoints.get(0);
                    b = nodePoints.get(1);
                } else {
                    a = nodePoints.get(nodePoints.size() - 2);
                    b = nodePoints.get(nodePoints.size() - 1);
                }
                gapDir = new Vec3d(b.x - a.x, b.y - a.y, b.z - a.z);
                gapLen = gapDir.length();
            }

            List<VecYPR> otherPoints = other.trackSingleGeometrySegment.pointsCache;
            VecYPR oa = otherPoints.get(other.index);
            VecYPR ob = otherPoints.get(other.index + 1);
            Vec3d localDir = new Vec3d(ob.x - oa.x, ob.y - oa.y, ob.z - oa.z);

            if (localDir.length() < 1e-9) {
                ImmersiveRailroading.error("searchAndAddGap: localDir is zero at other.index=%s, points.size=%s, node=%s, other=%s",
                        other.index, otherPoints.size(), node.id, other.id);
                return;
            }

            addNode(otherStartNode);
            addNode(otherEndNode);
            addEdge(other.id, otherStartNode.id, TrackEdge.EdgeType.NORMAL);
            addEdge(other.id, otherEndNode.id, TrackEdge.EdgeType.NORMAL);

            if (gapLen < 1e-9) {
                addEdge(other.id, otherStartNode.id, TrackEdge.EdgeType.NORMAL);
                return;
            }

            double cos = localDir.dotProduct(gapDir) / (localDir.length() * gapLen);
            if (cos > 0) {
                addEdge(other.id, otherEndNode.id, TrackEdge.EdgeType.NORMAL);
            } else {
                addEdge(other.id, otherStartNode.id, TrackEdge.EdgeType.NORMAL);
            }
        }
    }

    private void addNode(TrackNode node) {
        nodes.putIfAbsent(node.id, node);
    }

    private void addEdge(TrackNodeId from, TrackNodeId to, TrackEdge.EdgeType type) {
        if (from.equals(to)) return;
        TrackEdge edge = new TrackEdge(from, to, type);
        if (edges.add(edge)) {
            outgoing.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);
        }
    }

    protected void removeEdge(TrackEdge edge) {
        if (edges.remove(edge)) {
            List<TrackEdge> list = outgoing.get(edge.from);
            if (list != null) {
                list.remove(edge);
            }
        }
    }

    protected void removeTopologyFor(long regionId, Vec3i relBlockPos) {
        Set<TrackNodeId> affected = new HashSet<>();
        for (TrackNodeId id : nodes.keySet()) {
            if (id.regionPos == regionId && id.regionBlockPos.equals(relBlockPos)) {
                affected.add(id);
            }
        }
        if (affected.isEmpty()) return;

        // 收集所有涉及这些节点的边：出边 + 指向它们的所有入边
        Set<TrackEdge> toRemove = new HashSet<>();
        for (TrackNodeId id : affected) {
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) {
                toRemove.addAll(out);
            }
        }
        for (List<TrackEdge> list : outgoing.values()) {
            for (TrackEdge e : list) {
                if (affected.contains(e.to)) {
                    toRemove.add(e);
                }
            }
        }

        for (TrackEdge e : toRemove) {
            removeEdge(e);
        }

        for (TrackNodeId id : affected) {
            nodes.remove(id);
            outgoing.remove(id);
        }
    }

    protected void buildTopologyForRegion(TrackRegion region) {
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> entry : region.trackBlocks.entrySet()) {
            Vec3i absPos = region.toBlockPos(entry.getKey());
            buildTopologyFor(absPos, entry.getValue());
        }
    }

    public void buildAll() {
        for (TrackRegion region : regions.values()) {
            buildTopologyForRegion(region);
        }
    }

    //下面3个目前是客户端only
    public void rebuildTopologyAround(long regionId) {
        int rx = TrackRegionUtil.regionX(regionId);
        int rz = TrackRegionUtil.regionZ(regionId);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long id = ((long) (rx + dx) << 32) | ((rz + dz) & 0xFFFFFFFFL);
                TrackRegion region = regions.get(id);
                if (region != null) {
                    rebuildTopologyForRegion(region);
                }
            }
        }
    }

    protected void rebuildTopologyForRegion(TrackRegion region) {
        // 清掉本 region 的节点
        Set<TrackNodeId> regionNodes = new HashSet<>();
        for (TrackNodeId id : nodes.keySet()) {
            if (id.regionPos == region.regionPos) {
                regionNodes.add(id);
            }
        }
        for (TrackNodeId id : regionNodes) {
            // 清掉以它为一端的出边和指向它的入边
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) {
                for (TrackEdge e : new ArrayList<>(out)) {
                    removeEdge(e);
                }
            }
            for (List<TrackEdge> list : outgoing.values()) {
                for (TrackEdge e : new ArrayList<>(list)) {
                    if (e.to.equals(id)) {
                        removeEdge(e);
                    }
                }
            }
            nodes.remove(id);
            outgoing.remove(id);
        }

        // 重建
        buildTopologyForRegion(region);
    }

    private void researchNeighbors(Vec3i newAbsPos) {
        double radius = 4.0;
        double radiusSq = radius * radius;

        List<TrackNode> nearbyEndpoints = new ArrayList<>();
        for (TrackNode n : nodes.values()) {
            int lastIdx = n.trackSingleGeometrySegment.pointsCache.size() - 1;
            if (n.index != 0 && n.index != lastIdx) continue;
            Vec3i absPos = TrackRegionUtil.toBlockPos(n.id.regionPos, n.id.regionBlockPos);
            if (new Vec3d(newAbsPos.internal()).squareDistanceTo(new Vec3d(absPos.internal())) > radiusSq) continue;
            nearbyEndpoints.add(n);
        }

        for (TrackNode n : nearbyEndpoints) {
            TrackSingleGeometrySegment single = n.trackSingleGeometrySegment;
            boolean isNear = (n.index == 0);
            searchAndAddGap(single, isNear);
        }
    }
}