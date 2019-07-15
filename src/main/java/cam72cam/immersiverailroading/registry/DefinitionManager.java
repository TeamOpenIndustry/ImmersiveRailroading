package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.resource.Identifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

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

    public static void initDefinitions() throws IOException {
        initGauges();

        definitions = new LinkedHashMap<>();
        tracks = new LinkedHashMap<>();

        Set<String> defTypes = jsonLoaders.keySet();

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

        Identifier stock_json = new Identifier(ImmersiveRailroading.MODID, "rolling_stock/stock.json");

        inputs = stock_json.getResourceStreamAll();
        for (InputStream input : inputs) {

            JsonParser parser = new JsonParser();
            JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            int steps = 0;

            for (String defType : defTypes) {
                if (stock.has(defType)) {
                    steps += stock.get(defType).getAsJsonArray().size();
                }
            }

            Progress.Bar bar = Progress.push("Loading Models", steps);


            for (String defType : defTypes) {
                if (stock.has(defType)) {
                    for (JsonElement defName : stock.get(defType).getAsJsonArray()) {
                        bar.step(defName.getAsString());

                        if (blacklist.contains(defName.getAsString())) {
                            ImmersiveRailroading.info("Skipping blacklisted %s", defName.getAsString());
                            continue;
                        }
                        try {
                            String defID = String.format("rolling_stock/%s/%s.json", defType, defName.getAsString());
                            JsonObject data = getJsonData(defID);
                            definitions.put(defID, jsonLoaders.get(defType).apply(defID, data));
                        } catch (Exception ex) {
                            ImmersiveRailroading.catching(ex);
                        }
                    }
                }
            }

            Progress.pop(bar);
        }
        Progress.Bar bar = Progress.push("Generating Heightmap", definitions.size());

        for (EntityRollingStockDefinition def : definitions.values()) {
            bar.step(def.name());
            def.initHeightMap();
        }

        Progress.pop(bar);

        //Progress bar = ProgressManager.push("Loading tracks", )
        Identifier track_json = new Identifier(ImmersiveRailroading.MODID, "track/track.json");

        inputs = track_json.getResourceStreamAll();
        for (InputStream input : inputs) {

            JsonParser parser = new JsonParser();
            JsonObject track = parser.parse(new InputStreamReader(input)).getAsJsonObject();
            input.close();

            JsonArray types = track.getAsJsonArray("types");
            bar = Progress.push("Loading Tracks", types.size());

            for (JsonElement def : types) {
                bar.step(def.getAsString());
                String trackID = String.format("immersiverailroading:track/%s.json", def.getAsString());
                ImmersiveRailroading.info("Loading Track %s", trackID);
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
        ImmersiveRailroading.info("Loading stock " + defID);
        Identifier resource = new Identifier(ImmersiveRailroading.MODID, defID);

        InputStream input = resource.getResourceStream();

        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(new InputStreamReader(input)).getAsJsonObject();

        input.close();

        return result;
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

    @FunctionalInterface
    private interface JsonLoader {
        EntityRollingStockDefinition apply(String defID, JsonObject data) throws Exception;
    }

}
