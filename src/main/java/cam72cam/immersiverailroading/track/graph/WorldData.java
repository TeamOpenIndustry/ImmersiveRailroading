package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.net.offline.TrackRegionDeltaPacket;
import cam72cam.immersiverailroading.net.offline.TrackRegionPacket;
import cam72cam.immersiverailroading.net.offline.VehicleSyncPacket;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    // =============== 增量同步 ===============

    /**
     * 单个增量操作。regionBlockPos 使用 region 内坐标（512 内），
     * 序列化时复用 TrackMultiGeometrySegment 的 write/sizeBytes。
     */
    public static class DeltaOp {
        public enum Kind { PUT, REMOVE }

        public final Kind kind;
        public final Vec3i regionBlockPos;
        /** REMOVE 时为 null */
        public final TrackMultiGeometrySegment block;

        public DeltaOp(Kind kind, Vec3i regionBlockPos, TrackMultiGeometrySegment block) {
            this.kind = kind;
            this.regionBlockPos = regionBlockPos;
            this.block = block;
        }

        public static DeltaOp put(Vec3i regionBlockPos, TrackMultiGeometrySegment block) {
            return new DeltaOp(Kind.PUT, regionBlockPos, block);
        }

        public static DeltaOp remove(Vec3i regionBlockPos) {
            return new DeltaOp(Kind.REMOVE, regionBlockPos, null);
        }
    }

    /** 每个 tick 内按 region 聚合的待发增量。同 pos 后写覆盖先写。 */
    private final Map<Long, Map<Vec3i, DeltaOp>> pendingDeltas = new HashMap<>();

    /** 显式请求全量重发的 region（兜底通道，替代旧的 dirty 广播） */
    private final Set<Long> pendingFullSync = new HashSet<>();

    /** 外部可调用的兜底全量重发接口 */
    public void requestFullSync(long regionId) {
        synchronized (pendingFullSync) {
            pendingFullSync.add(regionId);
        }
    }

    private void queueDelta(long regionId, DeltaOp op) {
        synchronized (pendingDeltas) {
            pendingDeltas.computeIfAbsent(regionId, k -> new HashMap<>())
                    .put(op.regionBlockPos, op);
        }
    }

    /**
     * DeltaOp 序列化。放在 WorldData（同包）里以便访问 TrackMultiGeometrySegment 的
     * write/sizeBytes/构造，TrackRegionDeltaPacket 只调用这两个静态方法。
     */
    public static byte[] serializeDeltaOps(List<DeltaOp> ops) {
        int size = Integer.BYTES;
        for (DeltaOp op : ops) {
            size += Byte.BYTES + 3 * Integer.BYTES;
            if (op.kind == DeltaOp.Kind.PUT) {
                size += op.block.sizeBytes();
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.putInt(ops.size());
        for (DeltaOp op : ops) {
            buf.put((byte) op.kind.ordinal());
            buf.putInt(op.regionBlockPos.x);
            buf.putInt(op.regionBlockPos.y);
            buf.putInt(op.regionBlockPos.z);
            if (op.kind == DeltaOp.Kind.PUT) {
                op.block.write(buf);
            }
        }
        return buf.array();
    }

    public static List<DeltaOp> deserializeDeltaOps(byte[] raw, World world, long regionId) {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int count = buf.getInt();
        List<DeltaOp> ops = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            DeltaOp.Kind kind = DeltaOp.Kind.values()[buf.get() & 0xFF];
            Vec3i regionBlockPos = new Vec3i(buf.getInt(), buf.getInt(), buf.getInt());
            if (kind == DeltaOp.Kind.PUT) {
                TrackMultiGeometrySegment block =
                        new TrackMultiGeometrySegment(buf, world, regionId, regionBlockPos);
                ops.add(new DeltaOp(kind, regionBlockPos, block));
            } else {
                ops.add(new DeltaOp(kind, regionBlockPos, null));
            }
        }
        return ops;
    }

    // =============== 构造 / 加载 / 保存 ===============

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
                    } catch (Exception e) {
                        // 【修复】：不再抛出 RuntimeException 导致游戏崩溃
                        // 而是把坏掉的区域文件重命名隔离，并打印错误
                        ImmersiveRailroading.error("Track region file %s is corrupted! Renaming to .corrupted", file.getName());
                        ImmersiveRailroading.catching(e);
                        File badFile = new File(file.getParentFile(), file.getName() + ".corrupted");
                        if (badFile.exists()) badFile.delete();
                        file.renameTo(badFile);
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

    public boolean hasRegion(long id) {
        return regions.containsKey(id);
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

        // 服务端才入队；客户端 apply 增量时不会再回传
        if (world.isServer) {
            queueDelta(regionId, DeltaOp.put(TrackRegionUtil.toRegionBlockPos(pos), block));
        }
    }

    private void addTrackBlockToParents(Vec3i parentAbs, TrackMultiGeometrySegment block) {
        for (Map<Gauge, TrackSingleGeometrySegment> branch : block.paths) {
            for (TrackSingleGeometrySegment single : branch.values()) {
                Vec3i basePos = single.getAbsPos();
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
                Vec3i basePos = single.getAbsPos();
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

        if (world.isServer) {
            queueDelta(regionId, DeltaOp.remove(TrackRegionUtil.toRegionBlockPos(pos)));
        }
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
                    // 【修复】：先写临时文件
                    File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
                    TrackRegionUtil.writeBuffer(tmpFile, region.write());

                    // 【修复】：原子移动替换
                    try {
                        java.nio.file.Files.move(tmpFile.toPath(), file.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                        java.nio.file.Files.move(tmpFile.toPath(), file.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }

                    region.needsWriteToDisk = false;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void save(World world, File levelDirectory) {
        synchronized (LOADED) {
            if (LOADED.containsKey(world)) {
                LOADED.get(world).saveInternal();
            }
        }
    }

    public static void load(World world, File levelDirectory) {
        synchronized (LOADED) {
            if (LOADED.containsKey(world)) {
                ImmersiveRailroading.warn("World %s / %s already loaded!  This is a bug!",
                        levelDirectory.toString(), world.getId());
            }
            WorldData data = new WorldData(world, new File(levelDirectory, "immersiverailroading" + world.getId()));
            LOADED.put(world, data);

            data.buildAll();
        }
    }

    public static void unload(World world, File levelDirectory) {
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

    // =============== 服务端 tick：全量 + 增量 ===============

    private final Map<Player, Set<Long>> playerRegionMap = new HashMap<>();

    private void tick() {
        if (!world.isServer) {
            return;
        }

        List<Player> players = world.getEntities(Player.class);

        List<Player> removedPlayers = new ArrayList<>(playerRegionMap.keySet());
        removedPlayers.removeAll(players);
        for (Player removedPlayer : removedPlayers) {
            playerRegionMap.remove(removedPlayer);
            playerMapViewRegions.remove(removedPlayer); // 【新增】
        }

        // 1. 首见全量 / 卸载空包
        for (Player player : players) {
            Set<Long> sentRegions = playerRegionMap.computeIfAbsent(player, p -> new HashSet<>());
            Set<Long> trackingRegions = new HashSet<>();
            int radius = 16;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    trackingRegions.add(vecToRegion(new Vec3i(player.getPosition()).add(x * 16, 0, z * 16)));
                }
            }

            // 【新增】：合并地图请求的 Region，并清理过期的
            Map<Long, Integer> mapView = playerMapViewRegions.get(player);
            if (mapView != null) {
                trackingRegions.addAll(mapView.keySet());
            }

            // 处理移除：如果 sentRegions 里有，但 trackingRegions 里没了，发空包
            Set<Long> leftRegions = new HashSet<>(sentRegions);
            leftRegions.removeAll(trackingRegions);
            for (Long leftRegion : leftRegions) {
                sentRegions.remove(leftRegion);
                new TrackRegionPacket(world, leftRegion, null).sendToPlayer(player);
            }

            for (Long trackingRegion : trackingRegions) {
                TrackRegion region = regions.get(trackingRegion);

                // 【关键修复】：如果是地图请求的，且内存里没有，立即从磁盘加载！
                if (region == null && mapView != null && mapView.containsKey(trackingRegion)) {
                    ensureRegionLoaded(trackingRegion);
                    region = regions.get(trackingRegion);
                }

                if (region == null) {
                    if (sentRegions.contains(trackingRegion)) {
                        sentRegions.remove(trackingRegion);
                        new TrackRegionPacket(world, trackingRegion, null).sendToPlayer(player);
                    }
                    continue;
                }

                // 首见才发全量；dirty 广播已移除
                if (!sentRegions.contains(trackingRegion)) {
                    sentRegions.add(trackingRegion);
                    new TrackRegionPacket(world, trackingRegion, region).sendToPlayer(player);
                }
            }
        }

        // 2. 增量 flush（首见之后，保证客户端已收到基础全量）
        flushDeltas();

        // 3. 显式全量兜底请求
        flushFullSyncRequests();

        // 4. region 生命周期
        if (++regionRefreshCounter >= Config.ConfigRegion.refreshIntervalTicks) {
            regionRefreshCounter = 0;
            refreshRegions();
        }
    }

    private void flushDeltas() {
        Map<Long, Map<Vec3i, DeltaOp>> snapshot;
        synchronized (pendingDeltas) {
            if (pendingDeltas.isEmpty()) {
                return;
            }
            snapshot = new HashMap<>(pendingDeltas);
            pendingDeltas.clear();
        }

        for (Map.Entry<Long, Map<Vec3i, DeltaOp>> entry : snapshot.entrySet()) {
            long regionId = entry.getKey();
            Map<Vec3i, DeltaOp> ops = entry.getValue();
            if (ops.isEmpty()) continue;

            // region 已卸载：客户端会被首见逻辑走全量，增量直接丢弃
            if (!regions.containsKey(regionId)) continue;

            List<DeltaOp> opList = new ArrayList<>(ops.values());

            for (Map.Entry<Player, Set<Long>> e : playerRegionMap.entrySet()) {
                // 只发给已同步过该 region 的玩家；未同步者等首见全量
                if (e.getValue().contains(regionId)) {
                    new TrackRegionDeltaPacket(world, regionId, opList).sendToPlayer(e.getKey());
                }
            }
        }
    }

    private void flushFullSyncRequests() {
        Set<Long> copy;
        synchronized (pendingFullSync) {
            if (pendingFullSync.isEmpty()) {
                return;
            }
            copy = new HashSet<>(pendingFullSync);
            pendingFullSync.clear();
        }

        for (Long fullSyncId : copy) {
            TrackRegion region = regions.get(fullSyncId);
            if (region == null) continue;
            for (Map.Entry<Player, Set<Long>> e : playerRegionMap.entrySet()) {
                if (e.getValue().contains(fullSyncId)) {
                    new TrackRegionPacket(world, fullSyncId, region).sendToPlayer(e.getKey());
                }
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

            // 【新增】：把地图界面的请求也算作“需要”
            Map<Long, Integer> mapView = playerMapViewRegions.get(player);
            if (mapView != null) {
                required.addAll(mapView.keySet());
            }
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
                        long absR = TrackRegionUtil.vecToRegion(single.getAbsPos());
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

        // 只移除本 region 内 parent 的所有占位索引；
        // 其他 region 的 parent 跨越到本 region 的占位索引必须保留，否则 region 重载后跨 region 索引丢失
        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            Vec3i parentAbs = region.toBlockPos(e.getKey());
            removeTrackBlockFromParents(parentAbs, e.getValue());
        }

        // 该 region 已卸载，丢弃积压的增量与兜底请求，避免发给已不在追踪集合的客户端
        synchronized (pendingDeltas) {
            pendingDeltas.remove(r);
        }
        synchronized (pendingFullSync) {
            pendingFullSync.remove(r);
        }

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
        long siRegion = TrackRegionUtil.vecToRegion(block.getAbsPos());

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

            // 比较极端的情况：GAP 边中间穿过的 region 也加入覆盖，避免 initCurrent 在长 GAP 中点吸附失败
            Vec3d fp = new Vec3d(fromNode.point.x, fromNode.point.y, fromNode.point.z);
            Vec3d tp = new Vec3d(toNode.point.x,   toNode.point.y,   toNode.point.z);
            double len = fp.distanceTo(tp);
            int steps = Math.max(1, (int) Math.ceil(len / 8.0));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3i sample = new Vec3i(
                        fp.x + (tp.x - fp.x) * t,
                        fp.y + (tp.y - fp.y) * t,
                        fp.z + (tp.z - fp.z) * t
                );
                edge.coveringRegions.add(TrackRegionUtil.vecToRegion(sample));
            }
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
                Vec3i segPos = si.segment.getAbsPos();
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

    // =============== 客户端 region 加载 / 卸载 ===============

    public void clientUnloadRegion(long id) {
        synchronized (regions) {
            if (!regions.containsKey(id)) return;
            clearTopologyInRegion(id);
            trackBlockParentsByRegion.remove(id);
            regions.remove(id);
        }
    }

    public void clientLoadRegion(long id, TrackRegion region) {
        synchronized (regions) {
            if (regions.containsKey(id)) {
                clearTopologyInRegion(id);
                trackBlockParentsByRegion.remove(id);
            }
            regions.put(id, region);
        }

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

    // 记录玩家地图界面的请求 <Player, <RegionId, 过期时间>>
    private final Map<Player, Map<Long, Integer>> playerMapViewRegions = new HashMap<>();

    public void handleClientMapRequest(Player player, List<Long> requestRegions) {
        if (!world.isServer) return;

        if (requestRegions == null || requestRegions.isEmpty()) {
            // 客户端关闭地图，直接清理请求记录
            playerMapViewRegions.remove(player);
            return;
        }

        // 更新请求记录（不需要 serverTick 过期了，纯靠关闭包清理）
        Map<Long, Integer> viewRegions = playerMapViewRegions.computeIfAbsent(player, p -> new HashMap<>());
        viewRegions.clear();
        for (Long id : requestRegions) {
            viewRegions.put(id, 1); // 值随便填，只要存在就行
        }
    }

    // 供地图渲染使用的纯坐标缓存
    public List<Vec3d> clientMapVehicles = new ArrayList<>();

    public void updateClientVehicles(List<Vec3d> vehicles) {
        if (!world.isClient) return;
        this.clientMapVehicles.clear();
        this.clientMapVehicles.addAll(vehicles);
    }

    public void handleVehicleRequest(Player player, int minX, int minZ, int maxX, int maxZ) {
        if (!world.isServer) return;

        List<Vec3d> found = new ArrayList<>();
        // 这里为了稳妥，可以查 EntityMoveableRollingStock（或 EntityRollingStock）
        for (VehicleRef ref : vehicleRefs.values()) {
            if (ref.pos.x >= minX && ref.pos.x <= maxX && ref.pos.z >= minZ && ref.pos.z <= maxZ) {
                found.add(ref.pos);
            }
        }
        new VehicleSyncPacket(world, found).sendToPlayer(player);
    }
}