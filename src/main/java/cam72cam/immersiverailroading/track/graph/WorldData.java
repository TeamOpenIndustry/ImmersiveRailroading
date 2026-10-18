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

    // 按 parentAbsPos 所在 region 分桶的 node id 辅助索引
    private final Map<Long, Set<TrackNodeId>> nodeIdsByRegion = new HashMap<>();
    // 按 to 节点 parentAbsPos 所在 region 分桶的反向索引（哪些 from 有边指向该 region）
    private final Map<Long, Set<TrackNodeId>> outgoingByToRegion = new HashMap<>();
    // 按 si.segment 所在 region 分桶的 switch hub 索引
    private final Map<Long, Set<TrackNodeId>> switchHubIdsByRegion = new HashMap<>();

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

        buildTopologyFor(block);
        researchNeighbors(pos);
    }

    private void addTrackBlockToParents(Vec3i parentAbs, TrackMultiGeometrySegment block) {
        for (Map<Gauge, TrackSingleGeometrySegment> branch : block.paths) {
            for (TrackSingleGeometrySegment single : branch.values()) {
                Vec3i basePos = single.getBlockPos();
                for (Vec3i offset : single.positionsCache) {
                    Vec3i occupiedAbs = basePos.add(offset);
                    long r = TrackRegionUtil.vecToRegion(occupiedAbs);
                    trackBlockParentsByRegion
                            .computeIfAbsent(r, k -> new HashMap<>())
                            .computeIfAbsent(occupiedAbs, k -> new ArrayList<>())
                            .add(parentAbs);
                }
            }
        }
    }

    private void removeTrackBlockFromParents(Vec3i parentAbs, TrackMultiGeometrySegment block) {
        for (Map<Gauge, TrackSingleGeometrySegment> branch : block.paths) {
            for (TrackSingleGeometrySegment single : branch.values()) {
                Vec3i basePos = single.getBlockPos();
                for (Vec3i offset : single.positionsCache) {
                    Vec3i occupiedAbs = basePos.add(offset);
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

        Queue<Long> toCheck = new LinkedList<>(required);
        while (!toCheck.isEmpty()) {
            long r = toCheck.poll();
            TrackRegion region = regions.get(r);
            if (region == null) continue;

            for (TrackMultiGeometrySegment block : region.trackBlocks.values()) {
                for (Map<Gauge, TrackSingleGeometrySegment> branch : block.paths) {
                    for (TrackSingleGeometrySegment single : branch.values()) {
                        long absR = TrackRegionUtil.vecToRegion(single.getBlockPos());
                        long parentR = TrackRegionUtil.vecToRegion(single.parentAbsPos);

                        if (absR != parentR) {
                            if (absR != r && required.add(absR)) toCheck.add(absR);
                            if (parentR != r && required.add(parentR)) toCheck.add(parentR);
                        }
                    }
                }
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

    void ensureRegionLoaded(long r) {
        if (regions.containsKey(r)) return;
        if (directory == null) return;

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

            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                Vec3i parentAbs = region.toBlockPos(e.getKey());
                addTrackBlockToParents(parentAbs, e.getValue());
            }

            for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
                buildTopologyFor(e.getValue());
            }

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

        clearTopologyInRegion(r);

        trackBlockParentsByRegion.remove(r);

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

        regions.remove(r);
        ImmersiveRailroading.info("region %s,%s unloaded", TrackRegionUtil.regionX(r), TrackRegionUtil.regionZ(r));
    }

    private void clearTopologyInRegion(long regionId) {
        Set<TrackNodeId> regionNodes = nodeIdsByRegion.get(regionId);
        if (regionNodes == null || regionNodes.isEmpty()) return;
        Set<TrackNodeId> regionNodesCopy = new HashSet<>(regionNodes);

        Set<TrackEdge> toRemove = new HashSet<>();
        for (TrackNodeId id : regionNodesCopy) {
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) toRemove.addAll(out);
        }

        Set<TrackNodeId> fromIds = outgoingByToRegion.get(regionId);
        if (fromIds != null) {
            for (TrackNodeId fromId : fromIds) {
                List<TrackEdge> list = outgoing.get(fromId);
                if (list == null) continue;
                for (TrackEdge e : list) {
                    if (regionNodesCopy.contains(e.to)) toRemove.add(e);
                }
            }
        }

        removeSwitchesInRegion(regionId, null);

        for (TrackEdge e : toRemove) removeEdge(e);

        for (TrackNodeId id : regionNodesCopy) {
            removeNode(id);
            outgoing.remove(id);
        }
    }

    // =============== 拓扑构建 ===============

    protected void buildTopologyFor(TrackMultiGeometrySegment block) {
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
        long siRegion = TrackRegionUtil.vecToRegion(block.getBlockPos());

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

                addEdge(hub.id, turnStart.id, TrackEdge.EdgeType.GAP);
                addEdge(turnStart.id, hub.id, TrackEdge.EdgeType.GAP);

                turns.add(new SwitchInfo.Branch(turnStart.id, turnEnd.id));

                searchAndAddGap(turn, false);
            }

            SwitchInfo si = new SwitchInfo(hub.id, straightEnd.id, turns, block);
            switchByHub.put(hub.id, si);
            switchHubIdsByRegion.computeIfAbsent(siRegion, k -> new HashSet<>()).add(hub.id);
            for (SwitchInfo.Branch b : turns) {
                switchByTurn.put(b.start, si);
            }

            searchAndAddGap(straight, false);
            searchAndAddGap(straight, true);
        }
    }

    private void searchAndAddGap(TrackSingleGeometrySegment single, boolean isNear) {
        int lastIdx = single.pointsCache.size() - 1;
        if (lastIdx <= 0) return;

        TrackNode startNode = new TrackNode(single, 0, world);
        TrackNode endNode = new TrackNode(single, lastIdx, world);

        TrackNode node = isNear ? startNode : endNode;
        addNode(node);

        boolean forward = !isNear;
        TrackNode other = node.getConn(world, forward);
        if (other == null && single.switchIdex > 0 && !isNear) {
            other = node.getConn(world, false);
        }

        if (other == null || other.id.equals(node.id)) return;

        addNode(other);
        addEdge(node.id, other.id, TrackEdge.EdgeType.GAP);

        int otherLastIdx = other.trackSingleGeometrySegment.pointsCache.size() - 1;
        if (other.index != 0 && other.index != otherLastIdx) {
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
        if (nodes.putIfAbsent(node.id, node) == null) {
            long r = TrackRegionUtil.vecToRegion(node.trackSingleGeometrySegment.parentAbsPos);
            nodeIdsByRegion.computeIfAbsent(r, k -> new HashSet<>()).add(node.id);
        }
    }

    private void removeNode(TrackNodeId id) {
        TrackNode node = nodes.remove(id);
        if (node != null) {
            long r = TrackRegionUtil.vecToRegion(node.trackSingleGeometrySegment.parentAbsPos);
            Set<TrackNodeId> set = nodeIdsByRegion.get(r);
            if (set != null) {
                set.remove(id);
                if (set.isEmpty()) nodeIdsByRegion.remove(r);
            }
        }
    }

    private void addEdge(TrackNodeId from, TrackNodeId to, TrackEdge.EdgeType type) {
        if (from.equals(to)) return;
        TrackEdge edge = new TrackEdge(from, to, type);
        if (!edges.add(edge)) return;

        outgoing.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);

        TrackNode fromNode = nodes.get(from);
        TrackNode toNode = nodes.get(to);
        if (fromNode == null || toNode == null) return;

        long toRegion = TrackRegionUtil.vecToRegion(toNode.trackSingleGeometrySegment.parentAbsPos);
        outgoingByToRegion.computeIfAbsent(toRegion, k -> new HashSet<>()).add(from);

        TrackSingleGeometrySegment fromSeg = fromNode.trackSingleGeometrySegment;
        edge.coveringRegions.add(TrackRegionUtil.vecToRegion(fromSeg.absPos));
        edge.coveringRegions.add(TrackRegionUtil.vecToRegion(fromSeg.parentAbsPos));

        if (type == TrackEdge.EdgeType.GAP) {
            TrackSingleGeometrySegment toSeg = toNode.trackSingleGeometrySegment;
            edge.coveringRegions.add(TrackRegionUtil.vecToRegion(toSeg.absPos));
            edge.coveringRegions.add(TrackRegionUtil.vecToRegion(toSeg.parentAbsPos));
        }

        for (long r : edge.coveringRegions) {
            edgesByRegion.computeIfAbsent(r, k -> new HashSet<>()).add(edge);
        }
    }

    protected void removeEdge(TrackEdge edge) {
        if (!edges.remove(edge)) return;

        List<TrackEdge> list = outgoing.get(edge.from);
        if (list != null) list.remove(edge);

        TrackNode toNode = nodes.get(edge.to);
        if (toNode != null) {
            long toRegion = TrackRegionUtil.vecToRegion(toNode.trackSingleGeometrySegment.parentAbsPos);
            Set<TrackNodeId> set = outgoingByToRegion.get(toRegion);
            if (set != null) {
                // 只有该 from 不再有边指向 toRegion 时才移除
                List<TrackEdge> fromList = outgoing.get(edge.from);
                boolean stillHas = false;
                if (fromList != null) {
                    for (TrackEdge e : fromList) {
                        TrackNode eTo = nodes.get(e.to);
                        if (eTo == null) continue;
                        if (TrackRegionUtil.vecToRegion(eTo.trackSingleGeometrySegment.parentAbsPos) == toRegion) {
                            stillHas = true;
                            break;
                        }
                    }
                }
                if (!stillHas) {
                    set.remove(edge.from);
                    if (set.isEmpty()) outgoingByToRegion.remove(toRegion);
                }
            }
        }

        for (long r : edge.coveringRegions) {
            Set<TrackEdge> set = edgesByRegion.get(r);
            if (set != null) set.remove(edge);
        }
        edge.coveringRegions.clear();
    }

    private void removeSwitchesInRegion(long regionId, Vec3i relBlockPosOrNull) {
        Set<TrackNodeId> hubIds = switchHubIdsByRegion.get(regionId);
        if (hubIds == null) return;

        List<TrackNodeId> toRemove = new ArrayList<>();
        for (TrackNodeId hubId : hubIds) {
            SwitchInfo si = switchByHub.get(hubId);
            if (si == null) {
                toRemove.add(hubId);
                continue;
            }
            if (relBlockPosOrNull != null) {
                Vec3i segPos = si.segment.getBlockPos();
                if (!TrackRegionUtil.toRegionBlockPos(segPos).equals(relBlockPosOrNull)) continue;
            }
            toRemove.add(hubId);
        }

        for (TrackNodeId hubId : toRemove) {
            hubIds.remove(hubId);
            SwitchInfo si = switchByHub.remove(hubId);
            if (si != null) {
                for (SwitchInfo.Branch b : si.turns) {
                    switchByTurn.remove(b.start);
                }
            }
        }

        if (hubIds.isEmpty()) switchHubIdsByRegion.remove(regionId);
    }

    protected void removeTopologyFor(long regionId, Vec3i relBlockPos) {
        Set<TrackNodeId> regionNodes = nodeIdsByRegion.get(regionId);
        if (regionNodes == null) return;

        Set<TrackNodeId> affected = new HashSet<>();
        for (TrackNodeId id : regionNodes) {
            TrackNode node = nodes.get(id);
            if (node == null) continue;
            Vec3i parentAbs = node.trackSingleGeometrySegment.parentAbsPos;
            if (TrackRegionUtil.toRegionBlockPos(parentAbs).equals(relBlockPos)) {
                affected.add(id);
            }
        }
        if (affected.isEmpty()) return;

        Set<TrackEdge> toRemove = new HashSet<>();
        for (TrackNodeId id : affected) {
            List<TrackEdge> out = outgoing.get(id);
            if (out != null) toRemove.addAll(out);
        }

        Set<TrackNodeId> fromIds = outgoingByToRegion.get(regionId);
        if (fromIds != null) {
            for (TrackNodeId fromId : fromIds) {
                List<TrackEdge> list = outgoing.get(fromId);
                if (list == null) continue;
                for (TrackEdge e : list) {
                    if (affected.contains(e.to)) toRemove.add(e);
                }
            }
        }

        removeSwitchesInRegion(regionId, relBlockPos);

        for (TrackEdge e : toRemove) removeEdge(e);

        for (TrackNodeId id : affected) {
            removeNode(id);
            outgoing.remove(id);
        }
    }

    protected void buildTopologyForRegion(TrackRegion region) {
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> entry : region.trackBlocks.entrySet()) {
            buildTopologyFor(entry.getValue());
        }
    }

    public void buildAll() {
        for (TrackRegion region : regions.values()) {
            buildTopologyForRegion(region);
        }
    }

    private void researchNeighbors(Vec3i newAbsPos) {
        TrackMultiGeometrySegment newBlock = getTrackBlock(newAbsPos);
        if (newBlock == null) return;
        if (newBlock.paths.isEmpty()) return;

        double gaugeScale = newBlock.paths.get(0).values().iterator().next().gauge.scale();
        int hori = Math.max((int) (gaugeScale * 2), 1);
        int vert = 1;

        Set<TrackMultiGeometrySegment> nearbyBlocks = new LinkedHashSet<>();
        Set<Vec3i> scanned = new HashSet<>();

        for (Vec3i rel : newBlock.getPositionsCache()) {
            Vec3i coverAbs = newAbsPos.add(rel);
            for (int x = -hori; x <= hori; x++) {
                for (int y = -vert; y <= vert; y++) {
                    for (int z = -hori; z <= hori; z++) {
                        Vec3i scan = coverAbs.add(x, y, z);
                        if (!scanned.add(scan)) continue;

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
                for (TrackSingleGeometrySegment straight : block.paths.get(0).values()) {
                    if (straight.pointsCache.size() <= 1) continue;
                    searchAndAddGap(straight, true);
                    searchAndAddGap(straight, false);
                }
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

    public void clientUnloadRegion(long id) {
        synchronized (regions) {
            if (!regions.containsKey(id)) return;
            // clearTopologyInRegion 内部会通过 outgoingByToRegion 反向索引，
            // 把邻居指向本 region 的所有边一并清掉，不留幽灵边
            clearTopologyInRegion(id);
            trackBlockParentsByRegion.remove(id);
            regions.remove(id);
        }
        ImmersiveRailroading.info("region %s,%s unloaded",
                TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));
    }

    public void clientLoadRegion(long id, TrackRegion region) {
        synchronized (regions) {
            // 覆盖写入：先清旧拓扑再放新数据
            if (regions.containsKey(id)) {
                clearTopologyInRegion(id);
                trackBlockParentsByRegion.remove(id);
            }
            regions.put(id, region);
        }
        ImmersiveRailroading.info("region %s,%s loaded",
                TrackRegionUtil.regionX(id), TrackRegionUtil.regionZ(id));

        // 1. parent 占位索引（必须先建，searchAndAddGap 依赖它扫邻居）
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            addTrackBlockToParents(region.toBlockPos(e.getKey()), e.getValue());
        }

        // 2. 建本 region 自身的 NORMAL 边 + 内部 GAP 边
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            buildTopologyFor(e.getValue());
        }

        // 3. 唤醒邻居（不清邻居节点，只让邻居跑 searchAndAddGap 补 GAP 边）
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            researchNeighbors(region.toBlockPos(e.getKey()));
        }
    }
}