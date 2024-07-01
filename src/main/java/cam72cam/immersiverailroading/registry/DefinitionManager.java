package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.util.CAML;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.immersiverailroading.util.JSON;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefinitionManager {
    private static Map<String, EntityRollingStockDefinition> definitions;
    private static Map<String, TrackDefinition> tracks;
    public static Map<String, MultiblockDefinition> multiblocks;
    private static final Map<String, StockLoader> stockLoaders;

    static {
        stockLoaders = new LinkedHashMap<>();
        stockLoaders.put("locomotives", (String defID, DataBlock data) -> {
            String era = data.getValue("era").asString();
            switch (era) {
                case "steam":
                    return new LocomotiveSteamDefinition(defID, data);
                case "diesel":
                    return new LocomotiveDieselDefinition(defID, data);
                default:
                    ImmersiveRailroading.warn("Invalid era %s in %s", era, defID);
                    return null;
            }
        });

        stockLoaders.put("tender", TenderDefinition::new);
        stockLoaders.put("passenger", CarPassengerDefinition::new);
        stockLoaders.put("freight", CarFreightDefinition::new);
        stockLoaders.put("tank", CarTankDefinition::new);
        stockLoaders.put("hand_car", HandCarDefinition::new);
    }

    private static void initGauges() throws IOException {
        for (Gauge value : new ArrayList<>(Gauge.values())) {
            Gauge.remove(value.value());
        }

        List<DataBlock> blocks = new ArrayList<>();

        Identifier gauges_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/gauges.json");
        List<InputStream> inputs = gauges_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(JSON.parse(input));
        }

        Identifier gauges_caml = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/gauges.caml");
        inputs = gauges_caml.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(CAML.parse(input));
        }

        List<Double> toRemove = new ArrayList<>();

        for (DataBlock gauges : blocks) {
            DataBlock register = gauges.getBlock("register");
            if (register != null) {
                register.getValueMap().forEach((key, value) -> Gauge.register(value.asDouble(), key));
            }
            List<DataBlock.Value> remove = gauges.getValues("remove");
            if (remove != null) {
                for (DataBlock.Value gauge : remove) {
                    toRemove.add(gauge.asDouble());
                }
            }
        }

        for (double gauge : toRemove) {
            Gauge.remove(gauge);
        }
    }

    public static void initDefinitions() {
        if (definitions != null) {
            for (EntityRollingStockDefinition def : definitions.values()) {
                if (def.model != null) {
                    def.model.free();
                }
            }
        }

        if (tracks != null) {
            for (TrackDefinition def : tracks.values()) {
                for (TrackModel model : def.models) {
                    model.free();
                }
            }
        }

        if (multiblocks != null) {
            for (MultiblockDefinition def : multiblocks.values()) {
                if (def.model != null) {
                    def.model.free();
                }
            }
        }

        try {
            initGauges();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load gauges, do you have a broken pack?", e);
        }

        // Parallel streams use numCPUs-1 threads for stream workloads.
        Runtime runtime = Runtime.getRuntime();
        int processors = runtime.availableProcessors();

        // Manual garbage collection so we get an accurate quantity of free memory.
        runtime.gc();

        long maxMemory = runtime.maxMemory();
        if (maxMemory == Long.MAX_VALUE) {
            maxMemory = runtime.totalMemory();
        }
        ImmersiveRailroading.info("Detected %sMB of memory free", maxMemory/1024/1024);
        try {
            com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            maxMemory = Math.min(os.getFreePhysicalMemorySize() + runtime.totalMemory(), maxMemory);
            ImmersiveRailroading.info("Adjusted to %sMB of memory free", maxMemory/1024/1024);
        } catch (UnsatisfiedLinkError | Exception ex) {
            ImmersiveRailroading.catching(ex);
        }

        int loadingThreads = Math.max(1, Math.min(processors, (int) (maxMemory / (ConfigPerformance.megabytesReservedPerStockLoadingThread * 1024L * 1024L))));
        ImmersiveRailroading.info("Using %s threads to load Immersive Railroading (%sMB per thread)", loadingThreads, ConfigPerformance.megabytesReservedPerStockLoadingThread);
        ForkJoinPool stockLoadingPool = new ForkJoinPool(loadingThreads, pool -> {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("ImmersiveRailroading-" + worker.getPoolIndex());
            return worker;
        }, null, false);
        try {
            stockLoadingPool.submit(() -> {
                try {
                    initModels();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load rolling stock, do you have a broken pack?", e);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            // the pool broke in unexpected ways or was killed
            throw new RuntimeException(e);
        } finally {
            stockLoadingPool.shutdown();
        }
        try {
            initTracks();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load tracks, do you have a broken pack?", e);
        }
        try {
            initMultiblocks();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load multiblocks, do you have a broken pack?", e);
        }
    }

    private static void initModels() throws IOException {
        ImmersiveRailroading.info("Loading stock models.");

        Set<String> defTypes = stockLoaders.keySet();
        List<String> blacklist = getModelBlacklist(defTypes);

        LinkedHashMap<String, String> definitionIDMap = new LinkedHashMap<>();
        Map<String, DataBlock.Value> definitionIDPacks = new HashMap<>();

        List<DataBlock> blocks = new ArrayList<>();

        Identifier stock_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/stock.json");
        List<InputStream> inputs = stock_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(JSON.parse(input));
        }

        Identifier stock_caml = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/stock.caml");
        inputs = stock_caml.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(CAML.parse(input));
        }


        for (DataBlock stock : blocks) {
            for (String defType : defTypes) {
                List<DataBlock.Value> names = stock.getValues(defType);
                if (names != null) {
                    for (String defName : names.stream().map(DataBlock.Value::asString).collect(Collectors.toList())) {
                        if (blacklist.contains(defName)) {
                            ImmersiveRailroading.info("Skipping blacklisted %s", defName);
                            continue;
                        }

                        String defID = String.format("rolling_stock/%s/%s.json", defType, defName);
                        if (definitionIDMap.containsKey(defID)) {
                            continue;
                        }

                        definitionIDMap.put(defID, defType);
                        DataBlock.Value pack = stock.getValue("pack");
                        if (pack.asString() != null) {
                            definitionIDPacks.put(defID, pack);
                        }
                    }
                }
            }
        }

        Progress.Bar bar = Progress.push("Loading Models", definitionIDMap.size());

        Map<String, EntityRollingStockDefinition> loaded = getStockLoadingStream(definitionIDMap.entrySet()).map(tuple -> {
            String defID = tuple.getKey();
            String defType = tuple.getValue();

            ImmersiveRailroading.debug("Loading stock " + defID);
            Identifier resource = new Identifier(ImmersiveRailroading.MODID, defID);

            try {
                if (!resource.canLoad()) {
                    resource = new Identifier(resource.getDomain(), resource.getPath().replace(".json", ".caml"));
                }
                if (!resource.canLoad()) {
                    ImmersiveRailroading.error("Unable to load stock %s: file not found", defID);
                    return null;
                }

                DataBlock block = DataBlock.load(resource);

                if (definitionIDPacks.containsKey(defID) && block.getValue("pack").asString() == null) {
                    // This is kind of a nasty hack...
                    block.getValueMap().put("pack", definitionIDPacks.get(defID));
                }

                EntityRollingStockDefinition stockDefinition = stockLoaders.get(defType).apply(defID, block);

                Runtime runtime = Runtime.getRuntime();
                if (runtime.freeMemory() < runtime.maxMemory() * 0.25) {
                    System.out.println("GC");
                    System.gc();
                }

                return Pair.of(stockDefinition.defID, stockDefinition);
            } catch (Exception e) {
                ImmersiveRailroading.error("Error loading model %s of type %s", defID, defType);
                ImmersiveRailroading.catching(e);

                return null;
            } finally {
                synchronized (bar) {
                    bar.step(defID);
                }
            }
        }).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        definitions = new LinkedHashMap<>();
        definitionIDMap.keySet().stream().filter(loaded::containsKey).forEach(x -> definitions.put(x, loaded.get(x)));

        Progress.pop(bar);
    }

    private static List<String> getModelBlacklist(Set<String> defTypes) throws IOException {
        List<String> blacklist = new ArrayList<>();

        List<DataBlock> blocks = new ArrayList<>();

        Identifier blacklist_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/blacklist.json");
        List<InputStream> inputs = blacklist_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(JSON.parse(input));
        }

        Identifier blacklist_caml = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/blacklist.caml");
        inputs = blacklist_caml.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(CAML.parse(input));
        }

        for (DataBlock block : blocks) {
            for (String defType : defTypes) {
                List<DataBlock.Value> found = block.getValues(defType);
                if (found != null) {
                    blacklist.addAll(found.stream().map(DataBlock.Value::asString).collect(Collectors.toList()));
                }
            }
        }

        return blacklist;
    }

    private static void initTracks() throws IOException {
        tracks = new LinkedHashMap<>();

        ImmersiveRailroading.info("Loading tracks.");

        List<DataBlock> blocks = new ArrayList<>();

        Identifier track_json = new Identifier(ImmersiveRailroading.MODID, "track/track.json");
        List<InputStream> inputs = track_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(JSON.parse(input));
        }

        Identifier track_caml = new Identifier(ImmersiveRailroading.MODID, "track/track.caml");
        inputs = track_caml.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(CAML.parse(input));
        }


        for (DataBlock track : blocks) {
            List<String> types = track.getValues("types").stream().map(DataBlock.Value::asString).collect(Collectors.toList());
            Progress.Bar bar = Progress.push("Loading Tracks", types.size());

            for (String def : types) {
                bar.step(def);
                String trackID = String.format("immersiverailroading:track/%s.json", def);
                ImmersiveRailroading.debug("Loading Track %s", trackID);

                Identifier identifier = new Identifier(trackID);

                if (!identifier.canLoad()) {
                    identifier = new Identifier(identifier.getDomain(), identifier.getPath().replace(".json", ".caml"));
                }

                if (!identifier.canLoad()) {
                    ImmersiveRailroading.error("Unable to load track '%s': file not found", trackID);
                    continue;
                }

                DataBlock block = DataBlock.load(identifier);

                if (track.getValue("pack").asString() != null && block.getValue("pack").asString() != null) {
                    // Copy in the pack name if not specified
                    block.getValueMap().put("pack", track.getValue("pack"));
                }
                try {
                    tracks.put(trackID, new TrackDefinition(trackID, block));
                } catch (Exception e) {
                    ImmersiveRailroading.catching(e);
                }
            }

            Progress.pop(bar);
        }
    }

    private static void initMultiblocks() throws IOException {
        multiblocks = new LinkedHashMap<>();

        ImmersiveRailroading.info("Loading Multiblocks.");

        List<DataBlock> blocks = new ArrayList<>();

        Identifier mb_json = new Identifier(ImmersiveRailroading.MODID, "multiblocks/multiblock.json");
        List<InputStream> inputs = mb_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(JSON.parse(input));
        }

        Identifier mb_caml = new Identifier(ImmersiveRailroading.MODID, "multiblocks/multiblock.caml");
        inputs = mb_caml.getResourceStreamAll();
        for (InputStream input : inputs) {
            blocks.add(CAML.parse(input));
        }


        for (DataBlock multiblock : blocks) {
            List<String> types = multiblock.getValues("register").stream().map(DataBlock.Value::asString).collect(Collectors.toList());
            Progress.Bar bar = Progress.push("Loading Multiblocks", types.size());

            for (String def : types) {
                bar.step(def.toUpperCase());
                ImmersiveRailroading.debug("Loading Multiblock %s", def);

                Identifier identifier = new Identifier(ImmersiveRailroading.MODID, "multiblocks/"+def+".json");

                if (!identifier.canLoad()) {
                    identifier = new Identifier(identifier.getDomain(), identifier.getPath().replace(".json", ".caml"));
                }

                if (!identifier.canLoad()) {
                    ImmersiveRailroading.error("Unable to load multiblocks '%s': file not found", identifier.toString());
                    continue;
                }

                DataBlock block = DataBlock.load(identifier);

                if (multiblock.getValue("pack").asString() != null && block.getValue("pack").asString() != null) {
                    block.getValueMap().put("pack", multiblock.getValue("pack"));
                }
                try {
                    MultiblockDefinition definition = new MultiblockDefinition(def, block);
                    multiblocks.put(definition.name, definition);
                } catch (Exception e) {
                    ImmersiveRailroading.catching(e);
                }
            }
            Progress.pop(bar);
        }
    }

    /**
     * Get a stream for a collection that is used to load stocks in a singlethreaded or a multithreaded way.
     *
     * @param collection Collection of items.
     * @param <E> Type of item.
     * @return Singlethreaded or multithreaded stream.
     */
    private static <E> Stream<E> getStockLoadingStream(Collection<E> collection) {
        if (!ConfigPerformance.multithreadedStockLoading) {
            return collection.stream();
        }
        return collection.parallelStream();
    }

    public static EntityRollingStockDefinition getDefinition(String defID) {
        return definitions.get(defID);
    }

    public static Collection<EntityRollingStockDefinition> getDefinitions() {
        return definitions.values();
    }

    public static Set<String> getDefinitionNames() {
        return definitions.keySet();
    }

    public static List<TrackDefinition> getTracks() {
        return new ArrayList<>(tracks.values());
    }

    public static List<String> getTrackIDs() {
        return new ArrayList<>(tracks.keySet());
    }

    public static TrackModel getTrack(String track, double value) {
        return getTrack(track).getTrackForGauge(value);
    }

    public static TrackDefinition getTrack(String track) {
        TrackDefinition def = tracks.get(track);
        if (def == null) {
            def = tracks.values().stream().findFirst().get();
        }
        return def;
    }

    @FunctionalInterface
    private interface StockLoader {
        EntityRollingStockDefinition apply(String defID, DataBlock data) throws Exception;
    }

}
