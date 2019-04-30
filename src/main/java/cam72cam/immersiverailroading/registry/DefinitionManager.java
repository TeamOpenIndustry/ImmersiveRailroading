package cam72cam.immersiverailroading.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cam72cam.immersiverailroading.model.TrackModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.util.Identifier;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

public class DefinitionManager {

	private static Map<String, EntityRollingStockDefinition> definitions;
	private static Map<String, TrackDefinition> tracks;

	@FunctionalInterface
	private interface JsonLoader {
		EntityRollingStockDefinition apply(String defID, JsonObject data) throws Exception;
	}

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
		
		List<InputStream> inputs = ImmersiveRailroading.proxy.getResourceStreamAll(gauges_json);
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
		
		List<InputStream> inputs = ImmersiveRailroading.proxy.getResourceStreamAll(blacklist_json);
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
		
		inputs = ImmersiveRailroading.proxy.getResourceStreamAll(stock_json);
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
			
	        ProgressBar bar = ProgressManager.push("Loading Models", steps);
	        
	        
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
			
			ProgressManager.pop(bar);
		}
		ProgressBar bar = ProgressManager.push("Generating Heightmap", definitions.size());
		
		for (EntityRollingStockDefinition def : definitions.values()) {
			bar.step(def.name());
			def.initHeightMap();
		}
		
		ProgressManager.pop(bar);

		//ProgressBar bar = ProgressManager.push("Loading tracks", )
		Identifier track_json = new Identifier(ImmersiveRailroading.MODID, "track/track.json");

		inputs = ImmersiveRailroading.proxy.getResourceStreamAll(track_json);
		for (InputStream input : inputs) {

			JsonParser parser = new JsonParser();
			JsonObject track = parser.parse(new InputStreamReader(input)).getAsJsonObject();
			input.close();

			JsonArray types = track.getAsJsonArray("types");
			bar = ProgressManager.push("Loading Tracks", types.size());

			for (JsonElement def : types) {
				bar.step(def.getAsString());
				String trackID = String.format("immersiverailroading:track/%s.json", def.getAsString());
				ImmersiveRailroading.info("Loading Track %s", trackID);
				JsonParser trackParser = new JsonParser();
				JsonObject trackData = trackParser.parse(new InputStreamReader(ImmersiveRailroading.proxy.getResourceStream(new Identifier(trackID)))).getAsJsonObject();
				try {
					tracks.put(trackID, new TrackDefinition(trackID, trackData));
				} catch (Exception e) {
					ImmersiveRailroading.catching(e);
				}
			}

			ProgressManager.pop(bar);
		}
	}

	private static JsonObject getJsonData(String defID) throws IOException {
		ImmersiveRailroading.info("Loading stock " + defID);
		Identifier resource = new Identifier(ImmersiveRailroading.MODID, defID);
		
		InputStream input = ImmersiveRailroading.proxy.getResourceStream(resource);

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
		ArrayList<String> res = new ArrayList<>();
		res.addAll(tracks.keySet());
		return res;
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

}
