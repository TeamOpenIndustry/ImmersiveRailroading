package cam72cam.immersiverailroading.data;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldData {
    private final static Map<World, WorldData> LOADED = new HashMap<>();
    private final File directory;
    private final World world;
    final Map<Long, Region> regions;

    private WorldData(World world, File worldDirectory) {
        this.world = world;
        this.directory = worldDirectory;
        this.regions = new HashMap<>();

        if (worldDirectory != null && worldDirectory.exists()) {
            File[] files = worldDirectory.listFiles();
            if (files != null) {
                Arrays.stream(files).parallel().forEach(file -> {
                    long id = Long.parseLong(file.getName().replace(".irr", ""));
                    try {
                        Region region = new Region(Util.readBuffer(file));
                        synchronized (regions) {
                            regions.put(id, region);
                        }
                    } catch (IOException e) {
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
        return new File(directory, String.format("%s.irr", region));
    }

    public static long vecToRegion(Vec3i pos) {
        int factor = 8;
        long x = pos.x >> factor;
        long z = pos.z >> factor;
        return (x << 32) | z;
    }

    private Region getRegion(Vec3i pos, boolean create) {
        long id = vecToRegion(pos);
        return getRegionById(id, create);
    }

    private Region getRegionById(long id, boolean create) {
        synchronized (regions) {
            Region region = regions.get(id);
            if (region == null && create) {
                region = new Region();
                regions.put(id, region);
            }
            return region;
        }
    }

    public TrackBlock getTrackBlock(Vec3i pos) {
        Region region = getRegion(pos, false);
        if (region != null) {
            return region.getTrackBlock(pos);
        }
        return null;
    }

    public void setTrackBlock(Vec3i pos, TrackBlock block) {
        getRegion(pos, true).setTrackBlock(pos, block);
    }


    public TrackInfo getTrackInfo(TrackBlock block) {
        Region region = getRegionById(block.info_region, false);
        if (region != null) {
            return region.getTrackInfo(block.info_id);
        }
        return null;
    }

    public Collection<Region> getRegions() {
        return regions.values();
    }

    public TrackInfo allocateTrackInfo(RailInfo info, TrackInfo parent) {
        return getRegion(new Vec3i(info.placementInfo.placementPosition), true).allocateTrackInfo(info, parent);
    }

    public void updateTrackInfo(TrackInfo info) {
        getRegionById(info.region, true).updateTrackInfo(info);
    }

    private void save() {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException(String.format("Unable to create ImmersiveRailroading data directory %s!", directory));
            }
        }
        if (!directory.isDirectory()) {
            throw new RuntimeException(String.format("Expected ImmersiveRailroading data directory %s is not a directory!", directory));
        }

        for (Map.Entry<Long, Region> entry : regions.entrySet()) {
            if (entry.getValue().needsWriteToDisk) {
                try {
                    Util.writeBuffer(regionFile(entry.getKey()), entry.getValue().write());
                    entry.getValue().needsWriteToDisk = false;
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
                LOADED.get(world).save();
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
            LOADED.put(world, new WorldData(world, new File(levelDirectory, "immersiverailroading" + world.getId())));
        }
        ImmersiveRailroading.info("World %s / %s loaded in %sms", levelDirectory.toString(), world.getId(), System.currentTimeMillis() - st);
    }

    public static void unload(World world, File levelDirectory) {
        ImmersiveRailroading.info("Unload Level %s / %s", levelDirectory.toString(), world.getId());
        synchronized (LOADED) {
            LOADED.remove(world);
        }
    }

    public ITrack getTrackPath(Vec3i pos) {
        TrackBlock block = this.getTrackBlock(pos);
        if (block == null) {
            return null;
        }

        List<TrackInfo> infos = new ArrayList<>();
        while (block != null) {
            TrackInfo info = this.getTrackInfo(block);
            if (info != null) {
                infos.add(info);
            }
            block = block.replaced;
        }

        return new TrackMultiPath(this, infos);

        /*
        TrackInfo info = this.getTrackInfo(block);
        if (info == null) {
            return null;
        }

        return new TrackPath(info);*/
    }

    public TrackInfo getParent(TrackInfo info) {
        if (info.parent_id != -1) {
            Region region = getRegionById(info.parent_region, false);
            if (region != null) {
                return region.getTrackInfo(info.parent_id);
            }
        }
        return null;
    }

    public TrackInfo getTopParent(TrackInfo info) {
        while (true) {
            TrackInfo parent = getParent(info);
            if (parent == null) {
                return info;
            }
            info = parent;
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
                        trackingRegions.add(vecToRegion(new Vec3i(player.getPosition()).add(x*16, 0, z*16)));
                    }

                }
                for (Long trackingRegion : trackingRegions) {
                    Region region = regions.get(trackingRegion);
                    if (region != null) {
                        // new regions to the client should be checked every tick.
                        // modifications should probably be every 5-20 ticks?
                        if (!sentRegions.contains(trackingRegion) || region.modifiedSinceLastTick) {
                            sentRegions.add(trackingRegion);
                            new RegionPacket(world, trackingRegion, region).sendToPlayer(player);
                        }
                    }
                }
            }
            for (Region region : regions.values()) {
                region.modifiedSinceLastTick = false;
            }
        }
    }
}
