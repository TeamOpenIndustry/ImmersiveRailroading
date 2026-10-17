package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.TrackRegionPacket;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static cam72cam.immersiverailroading.track.graph.TrackRegionUtil.vecToRegion;

public class WorldData {
    private final World world;
    private final File directory;
    public final Map<Long, TrackRegion> regions;

    private final static Map<World, WorldData> LOADED = new HashMap<>();

    // 拓扑层
    public final Map<TrackNodeId, TrackNode> nodes = new HashMap<>();
    final Map<TrackNodeId, List<TrackEdge>> outgoing = new HashMap<>();
    public final Set<TrackEdge> edges = new HashSet<>();

    public final Map<Long, Set<TrackEdge>> edgesByRegion = new HashMap<>();

    // 按 region 分桶的占位方块索引
    public final Map<Long, Map<Vec3i, List<Vec3i>>> trackBlockParentsByRegion = new HashMap<>();

    // region 生命周期
    private final Map<Long, Integer> regionCooldown = new HashMap<>();
    private int regionRefreshCounter = 0;

    // switch
    final Map<TrackNodeId, SwitchInfo> switchByHub = new HashMap<>();
    final Map<TrackNodeId, SwitchInfo> switchByTurn = new HashMap<>();

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
                            ImmersiveRailroading.info("region %s,%s loaded", TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));
                        }
                    } catch (IOException | NumberFormatException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        for (TrackRegion region : regions.values()) {
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                Vec3i parentAbs = region.toBlockPos(e.getKey());
                addTrackBlockToParents(parentAbs, e.getValue());
            }
        }
    }

    public static WorldData get(World world) {
        return LOADED.get(world);
    }

    public static WorldData getOrCreate(World world) {
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
                ImmersiveRailroading.info("region %s,%s loaded", TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));
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
        ensureRegionLoaded(regionId);
        TrackRegion region = getRegionById(regionId, true);

        TrackMultiGeometrySegment old = region.getTrackBlock(pos);
        if (old != null) {
            removeTrackBlockFromParents(pos, old);
            removeTopologyFor(regionId, TrackRegionUtil.toRegionBlockPos(pos));
        }

        region.setTrackBlock(pos, block);
        addTrackBlockToParents(pos, block);

        buildTopologyFor(pos, block);
        researchNeighbors(pos);
    }

    private void addTrackBlockToParents(Vec3i parentAbs, TrackMultiGeometrySegment block) {
        for (Vec3i offset : block.getPositionsCache()) {
            Vec3i occupiedAbs = parentAbs.add(offset);
            long r = TrackRegionUtil.vecToRegion(occupiedAbs);
            trackBlockParentsByRegion
                    .computeIfAbsent(r, k -> new HashMap<>())
                    .computeIfAbsent(occupiedAbs, k -> new ArrayList<>())
                    .add(parentAbs);
        }
    }

    private void removeTrackBlockFromParents(Vec3i parentAbs, TrackMultiGeometrySegment block) {
        for (Vec3i offset : block.getPositionsCache()) {
            Vec3i occupiedAbs = parentAbs.add(offset);
            long r = TrackRegionUtil.vecToRegion(occupiedAbs);
            Map<Vec3i, List<Vec3i>> regionMap = trackBlockParentsByRegion.get(r);
            if (regionMap == null) continue;

            List<Vec3i> parents = regionMap.get(occupiedAbs);
            if (parents != null) {
                parents.remove(parentAbs);
                if (parents.isEmpty()) regionMap.remove(occupiedAbs);
            }
            if (regionMap.isEmpty()) trackBlockParentsByRegion.remove(r);
        }
    }

    public List<Vec3i> getParentsAt(Vec3i absBlock) {
        long r = TrackRegionUtil.vecToRegion(absBlock);
        Map<Vec3i, List<Vec3i>> regionMap = trackBlockParentsByRegion.get(r);
        if (regionMap == null) return null;
        return regionMap.get(absBlock);
    }

    public boolean removeTrackBlock(Vec3i pos) {
        long regionId = vecToRegion(pos);
        TrackRegion region = getRegionById(regionId, false);
        if (region == null) {
            return false;
        }
        TrackMultiGeometrySegment old = region.getTrackBlock(pos);
        if (!region.removeTrackBlock(pos)) {
            return false;
        }

        if (old != null) {
            removeTrackBlockFromParents(pos, old);
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
            LOADED.remove(world);
        }
    }

    public static void tick(World world) {
        WorldData data = get(world);
        if (data != null) {
            data.tick();
        }
    }

    private final Map<Player, Set<Long>> playerRegionMap = new HashMap<>();
    private void tick() {
        if (world.isServer) {
            List<Player> players = world.getEntities(Player.class);

            List<Player> removedPlayers = new ArrayList<>(playerRegionMap.keySet());
            removedPlayers.removeAll(players);
            for (Player removedPlayer : removedPlayers) {
                playerRegionMap.remove(removedPlayer);
            }

            for (Player player : players) {
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

            // region 生命周期刷新
            if (++regionRefreshCounter >= Config.ConfigRegion.refreshIntervalTicks) {
                regionRefreshCounter = 0;
                refreshRegions();
            }
        }
    }

    // =============== region 生命周期 ===============

    private void refreshRegions() {
        Set<Long> required = computeRequiredRegions();

        for (long r : required) {
            ensureRegionLoaded(r);
        }

        for (long r : new ArrayList<>(regions.keySet())) {
            if (required.contains(r)) {
                regionCooldown.put(r, Config.ConfigRegion.unloadDelayTicks);
            } else {
                int cd = regionCooldown.getOrDefault(r, 0) - Config.ConfigRegion.refreshIntervalTicks;
                if (cd <= 0) {
                    unloadRegion(r);
                    regionCooldown.remove(r);
                } else {
                    regionCooldown.put(r, cd);
                }
            }
        }
    }

    private Set<Long> computeRequiredRegions() {
        Set<Long> required = new HashSet<>();

        for (Player player : world.getEntities(Player.class)) {
            required.addAll(regionsInRadius(player.getPosition(), Config.ConfigRegion.playerRadius));
        }

        for (VehicleRef ref : vehicleRefs.values()) {
            double speed = ref.velocity.length();
            double radius = Config.ConfigRegion.vehicleRadiusMin
                    + speed * Config.ConfigRegion.vehicleRadiusPerSpeed;

            required.addAll(regionsInRadius(ref.pos, radius));

            if (speed > 0.1) {
                double ahead = speed * Config.ConfigRegion.vehicleLookAheadFactor
                        * Config.ConfigRegion.refreshIntervalTicks / 20.0;
                Vec3d dir = ref.velocity.scale(1.0 / speed);
                required.addAll(regionsInRadius(ref.pos.add(dir.scale(ahead)), radius));
            }
        }

        return required;
    }

    private Set<Long> regionsInRadius(Vec3d center, double radius) {
        int blockRadius = (int) Math.ceil(radius);
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        int minRX = (cx - blockRadius) >> 9;
        int maxRX = (cx + blockRadius) >> 9;
        int minRZ = (cz - blockRadius) >> 9;
        int maxRZ = (cz + blockRadius) >> 9;

        Set<Long> result = new HashSet<>();
        for (int rx = minRX; rx <= maxRX; rx++) {
            for (int rz = minRZ; rz <= maxRZ; rz++) {
                result.add(((long) rx << 32) | (rz & 0xFFFFFFFFL));
            }
        }
        return result;
    }

    private void ensureRegionLoaded(long r) {
        if (regions.containsKey(r)) return;
        if (directory == null) return;   // 客户端不加载磁盘

        File f = regionFile(r);
        if (!f.exists()) {
            return;
        }

        try {
            TrackRegion region = new TrackRegion(r, TrackRegionUtil.readBuffer(f), world);
            synchronized (regions) {
                regions.put(r, region);
                ImmersiveRailroading.info("region %s,%s loaded", TrackRegionUtil.regionX(r), TrackRegionUtil.regionZ(r));
            }

            // 1. parent 索引
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                Vec3i parentAbs = region.toBlockPos(e.getKey());
                addTrackBlockToParents(parentAbs, e.getValue());
            }

            // 2. 建 R 自身拓扑（含 R → 邻接 的 GAP）
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                Vec3i parentAbs = region.toBlockPos(e.getKey());
                buildTopologyFor(parentAbs, e.getValue());
            }

            // 3. 触发邻接重新发现 R（建立 邻接 → R 的 GAP）
            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                Vec3i parentAbs = region.toBlockPos(e.getKey());
                researchNeighbors(parentAbs);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void unloadRegion(long r) {
        TrackRegion region = regions.get(r);
        if (region == null) return;

        // 1. 清拓扑：from 或 to 在 r 里的所有边和节点
        clearTopologyInRegion(r);

        // 2. 清 parent 索引
        trackBlockParentsByRegion.remove(r);

        // 3. 需要持久化的写盘
        if (region.needsWriteToDisk && directory != null) {
            try {
                File file = regionFile(r);
                if (region.isEmpty()) {
                    if (file.exists() && !file.delete()) {
                        ImmersiveRailroading.warn("Unable to delete empty region file %s", file);
                    }
                } else {
                    TrackRegionUtil.writeBuffer(file, region.write());
                    region.needsWriteToDisk = false;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 4. 移除
        regions.remove(r);
        ImmersiveRailroading.info("region %s,%s unloaded", TrackRegionUtil.regionX(r), TrackRegionUtil.regionZ(r));
    }

    private void clearTopologyInRegion(long regionId) {
        Set<TrackNodeId> regionNodes = new HashSet<>();
        for (TrackNodeId id : nodes.keySet()) {
            if (id.regionPos == regionId) regionNodes.add(id);
        }
        if (regionNodes.isEmpty()) return;

        Set<TrackEdge> toRemove = new HashSet<>();
        for (TrackNodeId id : regionNodes) {
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) toRemove.addAll(out);
        }
        for (List<TrackEdge> list : outgoing.values()) {
            for (TrackEdge e : list) {
                if (regionNodes.contains(e.to)) toRemove.add(e);
            }
        }

        for (TrackEdge e : toRemove) removeEdge(e);
        for (TrackNodeId id : regionNodes) {
            nodes.remove(id);
            outgoing.remove(id);
        }
    }

    // =============== 拓扑构建 ===============

    protected void buildTopologyFor(Vec3i absPos, TrackMultiGeometrySegment block) {
        if (block.type == TrackItems.SWITCH && block.paths.size() >= 2) {
            buildSwitchTopology(block);
            return;
        }

        for (int pathIndex = 0; pathIndex < block.paths.size(); pathIndex++) {
            Map<Gauge, TrackSingleGeometrySegment> branch = block.paths.get(pathIndex);
            for (Map.Entry<Gauge, TrackSingleGeometrySegment> entry : branch.entrySet()) {
                TrackSingleGeometrySegment single = entry.getValue();
                int lastIdx = single.pointsCache.size() - 1;

                if (lastIdx > 0) {
                    TrackNode startNode = new TrackNode(single, 0, world);
                    addNode(startNode);
                    searchAndAddGap(single, false);

                    TrackNode endNode = new TrackNode(single, lastIdx, world);
                    addNode(endNode);
                    searchAndAddGap(single, true);

                    addEdge(startNode.id, endNode.id, TrackEdge.EdgeType.NORMAL);
                    addEdge(endNode.id, startNode.id, TrackEdge.EdgeType.NORMAL);
                }
            }
        }
    }

    private void buildSwitchTopology(TrackMultiGeometrySegment block) {
        Map<Gauge, TrackSingleGeometrySegment> straightBranch = block.paths.get(0);

        for (Map.Entry<Gauge, TrackSingleGeometrySegment> e : straightBranch.entrySet()) {
            Gauge gauge = e.getKey();
            TrackSingleGeometrySegment straight = e.getValue();
            int sLast = straight.pointsCache.size() - 1;
            if (sLast <= 0) continue;

            TrackNode hub = new TrackNode(straight, 0, world);
            TrackNode straightEnd = new TrackNode(straight, sLast, world);
            addNode(hub);
            addNode(straightEnd);

            addEdge(hub.id, straightEnd.id, TrackEdge.EdgeType.NORMAL);
            addEdge(straightEnd.id, hub.id, TrackEdge.EdgeType.NORMAL);

            List<SwitchInfo.Branch> turns = new ArrayList<>();

            for (int pathIndex = 1; pathIndex < block.paths.size(); pathIndex++) {
                TrackSingleGeometrySegment turn = block.paths.get(pathIndex).get(gauge);
                if (turn == null) continue;
                int tLast = turn.pointsCache.size() - 1;
                if (tLast <= 0) continue;

                TrackNode turnStart = new TrackNode(turn, 0, world);
                TrackNode turnEnd = new TrackNode(turn, tLast, world);
                addNode(turnStart);
                addNode(turnEnd);

                addEdge(turnStart.id, turnEnd.id, TrackEdge.EdgeType.NORMAL);
                addEdge(turnEnd.id, turnStart.id, TrackEdge.EdgeType.NORMAL);

                // 岔尖 GAP：hub ↔ turnStart
                addEdge(hub.id, turnStart.id, TrackEdge.EdgeType.GAP);
                addEdge(turnStart.id, hub.id, TrackEdge.EdgeType.GAP);

                turns.add(new SwitchInfo.Branch(turnStart.id, turnEnd.id));

                // 转弯腿出口的外部 GAP（turnStart 不跑）
                searchAndAddGap(turn, false);
            }

            SwitchInfo si = new SwitchInfo(hub.id, straightEnd.id, turns, block);
            switchByHub.put(hub.id, si);
            for (SwitchInfo.Branch b : turns) {
                switchByTurn.put(b.start, si);
            }

            // 直行腿两端外部 GAP
            searchAndAddGap(straight, false);
            searchAndAddGap(straight, true);
        }
    }

    private void searchAndAddGap(TrackSingleGeometrySegment single, boolean isNear) {
        int lastIdx = single.pointsCache.size() - 1;
        if (lastIdx <= 0) return;

        Vec3i absPos = single.getBlockPos();

        TrackNode startNode = new TrackNode(single, 0, world);
        TrackNode endNode = new TrackNode(single, lastIdx, world);

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
            TrackNode otherStartNode = new TrackNode(other.trackSingleGeometrySegment, 0, world);
            TrackNode otherEndNode = new TrackNode(other.trackSingleGeometrySegment, otherLastIdx, world);

            Vec3d gapDir = other.point.subtract(node.point);
            double gapLen = gapDir.length();
            if (gapLen < 1e-6) {
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
        if (!edges.add(edge)) return;

        outgoing.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);

        Vec3i fromAbs = TrackRegionUtil.toBlockPos(from.regionPos, from.regionBlockPos);
        if (type == TrackEdge.EdgeType.GAP) {
            Vec3i toAbs = TrackRegionUtil.toBlockPos(to.regionPos, to.regionBlockPos);
            edge.coveringRegions.addAll(regionsForParent(fromAbs));
            edge.coveringRegions.addAll(regionsForParent(toAbs));
        } else {
            edge.coveringRegions.addAll(regionsForParent(fromAbs));
        }

        for (long r : edge.coveringRegions) {
            edgesByRegion.computeIfAbsent(r, k -> new HashSet<>()).add(edge);
        }
    }

    protected void removeEdge(TrackEdge edge) {
        if (!edges.remove(edge)) return;

        List<TrackEdge> list = outgoing.get(edge.from);
        if (list != null) list.remove(edge);

        for (long r : edge.coveringRegions) {
            Set<TrackEdge> set = edgesByRegion.get(r);
            if (set != null) set.remove(edge);
        }
        edge.coveringRegions.clear();
    }

    protected void removeTopologyFor(long regionId, Vec3i relBlockPos) {
        Set<TrackNodeId> affected = new HashSet<>();
        for (TrackNodeId id : nodes.keySet()) {
            if (id.regionPos == regionId && id.regionBlockPos.equals(relBlockPos)) {
                affected.add(id);
            }
        }
        if (affected.isEmpty()) return;

        Set<TrackEdge> toRemove = new HashSet<>();
        for (TrackNodeId id : affected) {
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) toRemove.addAll(out);
        }
        for (List<TrackEdge> list : outgoing.values()) {
            for (TrackEdge e : list) {
                if (affected.contains(e.to)) toRemove.add(e);
            }
        }

        // 清理 switch 索引
        Set<SwitchInfo> removedSwitches = new HashSet<>();
        for (SwitchInfo si : switchByHub.values()) {
            boolean hit = affected.contains(si.hub) || affected.contains(si.straightEnd);
            if (!hit) {
                for (SwitchInfo.Branch b : si.turns) {
                    if (affected.contains(b.start) || affected.contains(b.end)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) removedSwitches.add(si);
        }
        for (SwitchInfo si : removedSwitches) {
            switchByHub.remove(si.hub);
            for (SwitchInfo.Branch b : si.turns) {
                switchByTurn.remove(b.start);
            }
        }

        for (TrackEdge e : toRemove) removeEdge(e);

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
        Set<TrackNodeId> regionNodes = new HashSet<>();
        for (TrackNodeId id : nodes.keySet()) {
            if (id.regionPos == region.regionPos) {
                regionNodes.add(id);
            }
        }
        for (TrackNodeId id : regionNodes) {
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

        // 同上 switch 清理逻辑，用 regionNodes 替代 affected
        Set<SwitchInfo> removedSwitches = new HashSet<>();
        for (SwitchInfo si : switchByHub.values()) {
            boolean hit = regionNodes.contains(si.hub) || regionNodes.contains(si.straightEnd);
            if (!hit) {
                for (SwitchInfo.Branch b : si.turns) {
                    if (regionNodes.contains(b.start) || regionNodes.contains(b.end)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) removedSwitches.add(si);
        }
        for (SwitchInfo si : removedSwitches) {
            switchByHub.remove(si.hub);
            for (SwitchInfo.Branch b : si.turns) {
                switchByTurn.remove(b.start);
            }
        }

        buildTopologyForRegion(region);
    }

    private void researchNeighbors(Vec3i newAbsPos) {
        TrackMultiGeometrySegment newBlock = getTrackBlock(newAbsPos);
        if (newBlock == null) return;
        if (newBlock.paths.isEmpty()) return;

        double gaugeScale = newBlock.paths.get(0).values().iterator().next().gauge.scale();
        int hori = Math.max((int) (gaugeScale * 2), 1);
        int vert = 1;

        Set<TrackMultiGeometrySegment> nearbyBlocks = new LinkedHashSet<>();

        for (Vec3i rel : newBlock.getPositionsCache()) {
            Vec3i coverAbs = newAbsPos.add(rel);
            for (int x = -hori; x <= hori; x++) {
                for (int y = -vert; y <= vert; y++) {
                    for (int z = -hori; z <= hori; z++) {
                        Vec3i scan = coverAbs.add(x, y, z);
                        List<Vec3i> parents = getParentsAt(scan);
                        if (parents == null) continue;
                        for (Vec3i parentAbs : parents) {
                            TrackMultiGeometrySegment block = getTrackBlock(parentAbs);
                            if (block != null) {
                                nearbyBlocks.add(block);
                            }
                        }
                    }
                }
            }
        }

        for (TrackMultiGeometrySegment block : nearbyBlocks) {
            if (block.type == TrackItems.SWITCH && block.paths.size() >= 2) {
                // 直行腿两端都跑
                for (TrackSingleGeometrySegment straight : block.paths.get(0).values()) {
                    if (straight.pointsCache.size() <= 1) continue;
                    searchAndAddGap(straight, true);
                    searchAndAddGap(straight, false);
                }
                // 转弯腿只跑 turnEnd 端，turnStart 由 hub↔turnStart 岔尖 GAP 专门处理
                for (int pi = 1; pi < block.paths.size(); pi++) {
                    for (TrackSingleGeometrySegment turn : block.paths.get(pi).values()) {
                        if (turn.pointsCache.size() <= 1) continue;
                        searchAndAddGap(turn, false);
                    }
                }
            } else {
                for (Map<Gauge, TrackSingleGeometrySegment> branch : block.paths) {
                    for (TrackSingleGeometrySegment single : branch.values()) {
                        if (single.pointsCache.size() <= 1) continue;
                        searchAndAddGap(single, true);
                        searchAndAddGap(single, false);
                    }
                }
            }
        }
    }

    private Set<Long> regionsForParent(Vec3i parentAbs) {
        TrackMultiGeometrySegment block = getTrackBlock(parentAbs);
        Set<Long> regions = new HashSet<>();
        if (block != null) {
            for (Vec3i offset : block.getPositionsCache()) {
                regions.add(TrackRegionUtil.vecToRegion(parentAbs.add(offset)));
            }
        }
        if (regions.isEmpty()) {
            regions.add(TrackRegionUtil.vecToRegion(parentAbs));
        }
        return regions;
    }

    // =============== 车辆引用 ===============

    public static class VehicleRef {
        public final Vec3d pos;
        public final Vec3d velocity;
        public VehicleRef(Vec3d pos, Vec3d velocity) { this.pos = pos; this.velocity = velocity; }
    }

    private final Map<UUID, VehicleRef> vehicleRefs = new HashMap<>();

    public void updateVehicleRefs(Map<UUID, VehicleRef> refs) {
        vehicleRefs.clear();
        vehicleRefs.putAll(refs);
    }

    public static class SwitchInfo {
        public static class Branch {
            public final TrackNodeId start;
            public final TrackNodeId end;
            public Branch(TrackNodeId start, TrackNodeId end) {
                this.start = start;
                this.end = end;
            }
        }

        public final TrackNodeId hub;
        public final TrackNodeId straightEnd;
        public final List<Branch> turns;
        public final TrackMultiGeometrySegment segment;

        public SwitchInfo(TrackNodeId hub, TrackNodeId straightEnd,
                          List<Branch> turns, TrackMultiGeometrySegment segment) {
            this.hub = hub;
            this.straightEnd = straightEnd;
            this.turns = turns;
            this.segment = segment;
        }
    }
}