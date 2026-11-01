package cam72cam.immersiverailroading.track.graph;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
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
    private final Map<Long, TrackRegion> regions;

    public Map<Long, TrackRegion> getRegions() {
        return regions;
    }

    private final static Map<World, WorldData> LOADED = new HashMap<>();

    public final Map<TrackNodeId, TrackNode> nodes = new HashMap<>();
    protected final Map<TrackNodeId, List<TrackEdge>> outgoing = new HashMap<>();
    public final Set<TrackEdge> edges = new HashSet<>();

    protected final Map<Long, Set<TrackEdge>> edgesByRegion = new HashMap<>();

    public final Map<Long, Map<Vec3i, List<Vec3i>>> trackBlockParentsByRegion = new HashMap<>();

    private final Map<Long, Set<TrackNodeId>> nodeIdsByRegion = new HashMap<>();
    private final Map<Long, Set<TrackNodeId>> outgoingByToRegion = new HashMap<>();
    private final Map<Long, Set<TrackNodeId>> switchHubIdsByRegion = new HashMap<>();

    private final Map<Long, Integer> regionCooldown = new HashMap<>();
    private int regionRefreshCounter = 0;

    // 仅保留 hub 一个 map
    protected final Map<TrackNodeId, SwitchInfo> switchByHub = new HashMap<>();

    // =============== 增量同步 ===============

    public static class DeltaOp {
        public enum Kind { PUT, REMOVE, SWITCH_STATE }

        public final Kind kind;
        public final Vec3i regionBlockPos;

        public final TrackMultiGeometrySegment block;

        public final SwitchState switchState;
        public final boolean offline;
        public final long currentTime;

        private DeltaOp(Kind kind, Vec3i pos,
                        TrackMultiGeometrySegment block,
                        SwitchState switchState, boolean offline, long currentTime) {
            this.kind = kind;
            this.regionBlockPos = pos;
            this.block = block;
            this.switchState = switchState;
            this.offline = offline;
            this.currentTime = currentTime;
        }

        public static DeltaOp put(Vec3i regionBlockPos, TrackMultiGeometrySegment block) {
            return new DeltaOp(Kind.PUT, regionBlockPos, block, null, false, 0);
        }

        public static DeltaOp remove(Vec3i regionBlockPos) {
            return new DeltaOp(Kind.REMOVE, regionBlockPos, null, null, false, 0);
        }

        public static DeltaOp switchState(Vec3i regionBlockPos,
                                          SwitchState switchState,
                                          boolean offline,
                                          long currentTime) {
            return new DeltaOp(Kind.SWITCH_STATE, regionBlockPos, null, switchState, offline, currentTime);
        }
    }

    private final Map<Long, Map<Vec3i, DeltaOp>> pendingDeltas = new HashMap<>();
    private final Set<Long> pendingFullSync = new HashSet<>();

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

    public static byte[] serializeDeltaOps(List<DeltaOp> ops) {
        int size = Integer.BYTES;
        for (DeltaOp op : ops) {
            size += Byte.BYTES + 3 * Integer.BYTES;
            switch (op.kind) {
                case PUT:          size += op.block.sizeBytes(); break;
                case SWITCH_STATE: size += Integer.BYTES + Byte.BYTES + Long.BYTES; break;
                case REMOVE:       break;
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.putInt(ops.size());
        for (DeltaOp op : ops) {
            buf.put((byte) op.kind.ordinal());
            buf.putInt(op.regionBlockPos.x);
            buf.putInt(op.regionBlockPos.y);
            buf.putInt(op.regionBlockPos.z);
            switch (op.kind) {
                case PUT:
                    op.block.write(buf);
                    break;
                case SWITCH_STATE:
                    buf.putInt(op.switchState.ordinal());
                    buf.put((byte) (op.offline ? 1 : 0));
                    buf.putLong(op.currentTime);
                    break;
                case REMOVE:
                    break;
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
            Vec3i pos = new Vec3i(buf.getInt(), buf.getInt(), buf.getInt());
            switch (kind) {
                case PUT: {
                    TrackMultiGeometrySegment block =
                            new TrackMultiGeometrySegment(buf, world, regionId, pos);
                    ops.add(DeltaOp.put(pos, block));
                    break;
                }
                case SWITCH_STATE: {
                    SwitchState state = SwitchState.values()[buf.getInt()];
                    boolean offline = buf.get() != 0;
                    long currentTime = buf.getLong();
                    ops.add(DeltaOp.switchState(pos, state, offline, currentTime));
                    break;
                }
                case REMOVE: {
                    ops.add(DeltaOp.remove(pos));
                    break;
                }
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

    public TrackMultiGeometrySegment getTrackBlockLoaded(Vec3i pos) {
        TrackRegion region = getRegion(pos, false);
        if (region != null) {
            return region.getTrackBlock(pos);
        }
        return null;
    }

    public void updateSwitchState(Vec3i pos, SwitchState switchState, boolean offline, long currentTime) {
        long regionId = vecToRegion(pos);
        ensureRegionLoaded(regionId);
        TrackRegion region = getRegionById(regionId, true);

        TrackMultiGeometrySegment seg = region.getTrackBlock(pos);
        if (seg != null) {
            seg.updateSwitchSate(switchState, offline, currentTime);
            region.needsWriteToDisk = true;
        }

        if (world.isServer) {
            queueDelta(regionId, DeltaOp.switchState(TrackRegionUtil.toRegionBlockPos(pos), switchState, offline, currentTime));
        }
    }

    public void clientApplySwitchStateDelta(long regionId, Vec3i regionBlockPos, SwitchState state, boolean offline, long currentTime) {
        if (!world.isClient) return;
        TrackRegion region = getRegionById(regionId, false);
        if (region == null) return;
        TrackMultiGeometrySegment seg = region.getTrackBlock(region.toBlockPos(regionBlockPos));
        if (seg != null) {
            seg.updateSwitchSate(state, offline, currentTime);
        }
    }

    public void updateTablePos(Vec3i pos, double tablePos, boolean offline, long currentTime) {
        long regionId = vecToRegion(pos);
        ensureRegionLoaded(regionId);
        TrackRegion region = getRegionById(regionId, true);

        TrackMultiGeometrySegment seg = region.getTrackBlock(pos);
        if (seg != null) {
            seg.updateTablePos(tablePos, offline, currentTime);
        }

        setTrackBlock(pos, seg);
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

        if (world.isServer || Config.ConfigRegion.clientBuildTopo) {
            buildTopologyFor(block);
            researchNeighbors(pos);
        }

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
                    File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
                    TrackRegionUtil.writeBuffer(tmpFile, region.write());

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
            playerMapViewRegions.remove(removedPlayer);
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

            Map<Long, Long> mapView = activeMapView(player);
            if (mapView != null) {
                trackingRegions.addAll(mapView.keySet());
            }

            Set<Long> leftRegions = new HashSet<>(sentRegions);
            leftRegions.removeAll(trackingRegions);
            for (Long leftRegion : leftRegions) {
                sentRegions.remove(leftRegion);
                new TrackRegionPacket(world, leftRegion, null).sendToPlayer(player);
            }

            for (Long trackingRegion : trackingRegions) {
                TrackRegion region = regions.get(trackingRegion);

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

                if (!sentRegions.contains(trackingRegion)) {
                    sentRegions.add(trackingRegion);
                    new TrackRegionPacket(world, trackingRegion, region).sendToPlayer(player);
                }
            }
        }

        flushDeltas();
        flushFullSyncRequests();

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

            if (!regions.containsKey(regionId)) continue;

            List<DeltaOp> opList = new ArrayList<>(ops.values());

            for (Map.Entry<Player, Set<Long>> e : playerRegionMap.entrySet()) {
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

            Map<Long, Long> mapView = activeMapView(player);
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

        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            Vec3i parentAbs = region.toBlockPos(e.getKey());
            removeTrackBlockFromParents(parentAbs, e.getValue());
        }

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

    /**
     * 道岔拓扑：
     *   hub ─── NORMAL ─── straightEnd
     *    ⇅ tie GAP
     *  turnStart ── NORMAL ── turnEnd
     *
     * 只有 hub 参与 switchByHub（岔心是唯一需要 switchState 决策的节点）。
     * turnStart 只有两条方向相反的边（turnEnd / hub），普通 cos 处理零歧义。
     * turnStart 不允许外接（不给它调 searchAndAddGap）。
     */
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

            List<TrackNodeId> turnStarts = new ArrayList<>();

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

                // tie GAP：hub ↔ turnStart（道岔切换点）
                addEdge(hub.id, turnStart.id, TrackEdge.EdgeType.GAP);
                addEdge(turnStart.id, hub.id, TrackEdge.EdgeType.GAP);

                turnStarts.add(turnStart.id);

                // turnStart 不外接；只给 turnEnd 向外接
                searchAndAddGap(turn, false);
            }

            switchByHub.put(hub.id, new SwitchInfo(straightEnd.id, turnStarts, block.getAbsPos()));
            switchHubIdsByRegion.computeIfAbsent(siRegion, k -> new HashSet<>()).add(hub.id);

            searchAndAddGap(straight, false);   // straightEnd 外接
            searchAndAddGap(straight, true);    // hub 外接
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
                if (!TrackRegionUtil.toRegionBlockPos(si.blockPos).equals(relBlockPosOrNull)) continue;
            }
            toRemove.add(hubId);
        }

        for (TrackNodeId hubId : toRemove) {
            hubIds.remove(hubId);
            switchByHub.remove(hubId);
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
        TrackMultiGeometrySegment newBlock = getTrackBlockLoaded(newAbsPos);
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
                            TrackMultiGeometrySegment block = getTrackBlockLoaded(parentAbs);
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

    /**
     * 道岔信息。只有 hub（岔心）需要，因为只有岔心是 3 条边（直轨、tie GAP、外部 GAP）的三岔口。
     * 直轨远端用 straightEnd 标识，弯轨起点用 turnStarts 标识。
     * blockPos 用于动态查最新 switchState，避免 segment 引用 stale。
     */
    public static class SwitchInfo {
        public final TrackNodeId straightEnd;
        public final List<TrackNodeId> turnStarts;
        public final Vec3i blockPos;

        public SwitchInfo(TrackNodeId straightEnd,
                          List<TrackNodeId> turnStarts,
                          Vec3i blockPos) {
            this.straightEnd = straightEnd;
            this.turnStarts = turnStarts;
            this.blockPos = blockPos;
        }

        public SwitchState getState(WorldData data) {
            TrackMultiGeometrySegment b = data.getTrackBlockLoaded(blockPos);
            return b != null ? b.getSwitchStateEnum() : SwitchState.STRAIGHT;
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

        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            addTrackBlockToParents(region.toBlockPos(e.getKey()), e.getValue());
        }

        if(!Config.ConfigRegion.clientBuildTopo) return;

        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            buildTopologyFor(e.getValue());
        }

        for (Map.Entry<Vec3i, TrackMultiGeometrySegment> e : region.trackBlocks.entrySet()) {
            researchNeighbors(region.toBlockPos(e.getKey()));
        }
    }

    private final Map<Player, Map<Long, Long>> playerMapViewRegions = new HashMap<>();

    public void handleClientMapRequest(Player player, List<Long> requestRegions) {
        if (!world.isServer) return;

        if (requestRegions == null || requestRegions.isEmpty()) {
            playerMapViewRegions.remove(player);
            return;
        }

        long currentTick = world.getTicks();
        Map<Long, Long> viewRegions = playerMapViewRegions.computeIfAbsent(player, p -> new HashMap<>());
        for (Long id : requestRegions) {
            viewRegions.put(id, currentTick + Config.ConfigRegion.unloadDelayTicks);
        }
        viewRegions.entrySet().removeIf(e -> e.getValue() < currentTick);
    }

    private Map<Long, Long> activeMapView(Player player) {
        long currentTick = world.getTicks();
        Map<Long, Long> mapView = playerMapViewRegions.get(player);
        if (mapView == null) return null;
        mapView.entrySet().removeIf(e -> e.getValue() < currentTick);
        return mapView.isEmpty() ? null : mapView;
    }

    public List<Vec3d> clientMapVehicles = new ArrayList<>();

    public void updateClientVehicles(List<Vec3d> vehicles) {
        if (!world.isClient) return;
        this.clientMapVehicles.clear();
        this.clientMapVehicles.addAll(vehicles);
    }

    public void handleVehicleRequest(Player player, int minX, int minZ, int maxX, int maxZ) {
        if (!world.isServer) return;

        List<Vec3d> found = new ArrayList<>();
        for (VehicleRef ref : vehicleRefs.values()) {
            if (ref.pos.x >= minX && ref.pos.x <= maxX && ref.pos.z >= minZ && ref.pos.z <= maxZ) {
                found.add(ref.pos);
            }
        }
        new VehicleSyncPacket(world, found).sendToPlayer(player);
    }
}