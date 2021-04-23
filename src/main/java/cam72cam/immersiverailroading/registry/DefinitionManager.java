package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefinitionManager {

    /**
     * How much memory in MiB does the loading of a stock take.
     * This is used to determine whether loading stock in a multithreaded way is possible.
     */
    private static final int STOCK_LOAD_MEMORY_PER_PROCESSOR = 50;

    private static Map<String, EntityRollingStockDefinition> definitions;
    private static Map<String, TrackDefinition> tracks;
    private static Map<String, JsonLoader> jsonLoaders;

    static {
        jsonLoaders = new LinkedHashMap<>();
        jsonLoaders.put("locomotives", (String defID, JsonObject data) -> {
            String era = data.get("era").getAsString();
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

        jsonLoaders.put("tender", TenderDefinition::new);
        jsonLoaders.put("passenger", CarPassengerDefinition::new);
        jsonLoaders.put("freight", CarFreightDefinition::new);
        jsonLoaders.put("tank", CarTankDefinition::new);
        jsonLoaders.put("hand_car", HandCarDefinition::new);
    }

    private static void initGauges() throws IOException {
        Identifier gauges_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/gauges.json");

        List<Double> toRemove = new ArrayList<>();

        List<InputStream> inputs = gauges_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            JsonParser parser = new JsonParser();
            JsonObject gauges = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            if (gauges.has("register")) {
                for (Entry<String, JsonElement> gauge : gauges.get("register").getAsJsonObject().entrySet()) {
                    Gauge.register(gauge.getValue().getAsDouble(), gauge.getKey());
                }
            }
            if (gauges.has("remove")) {
                for (JsonElement gauge : gauges.get("remove").getAsJsonArray()) {
                    toRemove.add(gauge.getAsDouble());
                }
            }
        }

        for (double gauge : toRemove) {
            Gauge.remove(gauge);
        }
    }

    public static void initDefinitions() throws IOException {
        initGauges();

        definitions = new LinkedHashMap<>();
        tracks = new LinkedHashMap<>();

        initModels();
        initTracks();
    }

    private static void initModels() throws IOException {
        ImmersiveRailroading.info("Loading stock models.");

        Set<String> defTypes = jsonLoaders.keySet();
        List<String> blacklist = getModelBlacklist(defTypes);

        LinkedHashMap<String, String> definitionIDMap = new LinkedHashMap<>();
        Identifier stock_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/stock.json");
        List<InputStream> inputs = stock_json.getResourceStreamAll();
        for (InputStream input : inputs) {

            JsonParser parser = new JsonParser();
            JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            for (String defType : defTypes) {
                if (stock.has(defType)) {
                    for (JsonElement defName : stock.get(defType).getAsJsonArray()) {
                        if (blacklist.contains(defName.getAsString())) {
                            ImmersiveRailroading.info("Skipping blacklisted %s", defName.getAsString());
                            continue;
                        }

                        String defID = String.format("rolling_stock/%s/%s.json", defType, defName.getAsString());
                        if (definitionIDMap.containsKey(defID)) {
                            continue;
                        }

                        definitionIDMap.put(defID, defType);
                    }
                }
            }
        }

        Progress.Bar bar = Progress.push("Loading Models", definitionIDMap.size());

        Map<String, EntityRollingStockDefinition> loaded = getStockLoadingStream(definitionIDMap.entrySet()).map(tuple -> {
            String defID = tuple.getKey();
            String defType = tuple.getValue();

            try {
                EntityRollingStockDefinition stockDefinition = jsonLoaders.get(defType).apply(defID, getJsonData(defID));

                synchronized (bar) {
                    bar.step(stockDefinition.name());
                }
                return Pair.of(stockDefinition.defID, stockDefinition);
            } catch (Exception e) {
                ImmersiveRailroading.error("Error loading model %s of type %s", defID, defType);
                ImmersiveRailroading.catching(e);

                synchronized (bar) {
                    // Important so that progress bar steps correctly.
                    bar.step("");
                }
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        definitionIDMap.keySet().stream().filter(loaded::containsKey).forEach(x -> definitions.put(x, loaded.get(x)));

        Progress.pop(bar);
    }

    private static List<String> getModelBlacklist(Set<String> defTypes) throws IOException {
        List<String> blacklist = new ArrayList<>();
        Identifier blacklist_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/blacklist.json");

        List<InputStream> inputs = blacklist_json.getResourceStreamAll();
        for (InputStream input : inputs) {
            JsonParser parser = new JsonParser();
            JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            for (String defType : defTypes) {
                if (stock.has(defType)) {
                    for (JsonElement defName : stock.get(defType).getAsJsonArray()) {
                        blacklist.add(defName.getAsString());
                    }
                }
            }
        }

        return blacklist;
    }

    private static void initTracks() throws IOException {
        ImmersiveRailroading.info("Loading tracks.");
        Identifier track_json = new Identifier(ImmersiveRailroading.MODID, "track/track.json");

        List<InputStream> inputs = track_json.getResourceStreamAll();
        for (InputStream input : inputs) {

            JsonParser parser = new JsonParser();
            JsonObject track = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            JsonArray types = track.getAsJsonArray("types");
            Progress.Bar bar = Progress.push("Loading Tracks", types.size());

            for (JsonElement def : types) {
                bar.step(def.getAsString());
                String trackID = String.format("immersiverailroading:track/%s.json", def.getAsString());
                ImmersiveRailroading.debug("Loading Track %s", trackID);
                JsonParser trackParser = new JsonParser();
                JsonObject trackData = trackParser.parse(new InputStreamReader(new Identifier(trackID).getResourceStream())).getAsJsonObject();
                try {
                    tracks.put(trackID, new TrackDefinition(trackID, trackData));
                } catch (Exception e) {
                    ImmersiveRailroading.catching(e);
                }
            }

            Progress.pop(bar);
        }
    }

    private static JsonObject getJsonData(String defID) throws IOException {
        ImmersiveRailroading.debug("Loading stock " + defID);
        Identifier resource = new Identifier(ImmersiveRailroading.MODID, defID);

        InputStream input = resource.getResourceStream();

        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(new InputStreamReader(input)).getAsJsonObject();

        input.close();

        return result;
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

        // Parallel streams use numCPUs-1 threads for stream workloads.
        Runtime runtime = Runtime.getRuntime();
        int processors = runtime.availableProcessors() - 1;
        if (processors <= 1) {
            return collection.stream();
        }

        // Manual garbage collection so we get an accurate quantity of free memory.
        runtime.gc();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        if (maxMemory == Long.MAX_VALUE) {
            maxMemory = totalMemory;
        }

        long freeMemory = runtime.freeMemory();
        freeMemory += maxMemory - totalMemory;

        long usedMemory = processors * STOCK_LOAD_MEMORY_PER_PROCESSOR * 1024 * 1024;
        return usedMemory < freeMemory ? collection.parallelStream() : collection.stream();
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
    private interface JsonLoader {
        EntityRollingStockDefinition apply(String defID, JsonObject data) throws Exception;
    }

}
