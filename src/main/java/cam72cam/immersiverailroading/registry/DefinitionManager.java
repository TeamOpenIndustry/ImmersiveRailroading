package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.task.GenerateDefinitionHeightMapsTask;
import cam72cam.immersiverailroading.registry.task.ParseDefinitionsTask;
import cam72cam.immersiverailroading.util.ConcurrencyUtil;
import cam72cam.immersiverailroading.util.ListUtil;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Tuple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DefinitionManager {

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

    public static void initDefinitions() throws IOException, ExecutionException {
        initGauges();

        definitions = new LinkedHashMap<>();
        tracks = new LinkedHashMap<>();

        int threadPoolSize = ConcurrencyUtil.calcThreadPoolSize();
        ExecutorService service = ConcurrencyUtil.makeThreadPool(threadPoolSize);
        CompletionService<Object> completionService = ConcurrencyUtil.makeCompletionService(service);

        try {
            initModels(completionService, threadPoolSize);
            initModelHeightMaps(completionService, threadPoolSize);
        } catch (InterruptedException e) {
            ImmersiveRailroading.error("Initializing stock definitions has been interrupted.");
            ConcurrencyUtil.shutdownService(service);
            Thread.currentThread().interrupt();
            return;
        }

        ConcurrencyUtil.shutdownService(service);

        initTracks();
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

    public static Collection<TrackDefinition> getTracks() {
        return tracks.values();
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

    private static void initModels(CompletionService<Object> completionService, int threadPoolSize) throws IOException, InterruptedException, ExecutionException {
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

        ArrayList<Tuple<String, String>> definitionList = new ArrayList<>(definitionIDMap.size());
        for (Entry<String, String> entry : definitionIDMap.entrySet()) {
            definitionList.add(new Tuple<>(entry.getKey(), entry.getValue()));
        }

        Progress.Bar bar = Progress.push("Loading Models", definitionIDMap.size());

        List<List<Tuple<String, String>>> definitionListParts = ListUtil.splitListIntoNParts(definitionList, threadPoolSize);
        for (List<Tuple<String, String>> definitions : definitionListParts) {
            completionService.submit(new ParseDefinitionsTask(definitions, jsonLoaders));
        }

        int taskCount = definitionListParts.size();
        for (int receivedTasks = 0; receivedTasks < taskCount; receivedTasks++) {
            Future<Object> future = completionService.take();

            //noinspection rawtypes
            Collection stockDefinitions = (Collection) future.get();
            for (Object o : stockDefinitions) {
                EntityRollingStockDefinition stockDefinition = (EntityRollingStockDefinition) o;
                bar.step(stockDefinition.name());
                definitions.put(stockDefinition.defID, stockDefinition);
            }
        }

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

    private static void initModelHeightMaps(CompletionService<Object> completionService, int threadPoolSize) throws InterruptedException {
        ImmersiveRailroading.info("Generating height maps.");

        List<EntityRollingStockDefinition> stockDefinitionList = new ArrayList<>(definitions.values());
        List<List<EntityRollingStockDefinition>> definitionListParts = ListUtil.splitListIntoNParts(stockDefinitionList, threadPoolSize);

        Progress.Bar bar = Progress.push("Generating Heightmap", definitionListParts.size());

        for (List<EntityRollingStockDefinition> stockDefinitions : definitionListParts) {
            completionService.submit(new GenerateDefinitionHeightMapsTask(stockDefinitions), null);
        }

        int taskCount = definitionListParts.size();
        for (int receivedTasks = 0; receivedTasks < taskCount; receivedTasks++) {
            bar.step("Batch " + receivedTasks + 1);
            completionService.take();
        }

        Progress.pop(bar);
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

    @FunctionalInterface
    public interface JsonLoader {
        EntityRollingStockDefinition apply(String defID, JsonObject data) throws Exception;
    }

}
