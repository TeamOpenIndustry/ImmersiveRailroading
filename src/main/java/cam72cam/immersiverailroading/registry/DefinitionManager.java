package cam72cam.immersiverailroading.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

public class DefinitionManager {

	private static Map<String, EntityRollingStockDefinition> definitions;
	
	@FunctionalInterface
	private static interface JsonLoader {
		EntityRollingStockDefinition apply(String defID, JsonObject data) throws Exception;
	}; 
	
	private static Map<String, JsonLoader> jsonLoaders;
	
	static {
		jsonLoaders = new LinkedHashMap<String, JsonLoader>();
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

		jsonLoaders.put("tender", (String defID, JsonObject data) -> new TenderDefinition(defID, data));
		jsonLoaders.put("passenger", (String defID, JsonObject data) -> new CarPassengerDefinition(defID, data));
		jsonLoaders.put("freight", (String defID, JsonObject data) -> new CarFreightDefinition(defID, data));
		jsonLoaders.put("tank", (String defID, JsonObject data) -> new CarTankDefinition(defID, data));
		jsonLoaders.put("hand_car", (String defID, JsonObject data) -> new HandCarDefinition(defID, data));
	}

	public static void initDefinitions() throws IOException {
		definitions = new LinkedHashMap<String, EntityRollingStockDefinition>();
		
		Set<String> defTypes = jsonLoaders.keySet();
		
		List<String> blacklist = new ArrayList<String>();
		
		ResourceLocation blacklist_json = new ResourceLocation(ImmersiveRailroading.MODID, "rolling_stock/blacklist.json");
		
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
		
		ResourceLocation stock_json = new ResourceLocation(ImmersiveRailroading.MODID, "rolling_stock/stock.json");
		
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
			
	        ProgressBar bar = ProgressManager.push("Generating Heightmaps", steps);
	        
	        
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
	}

	private static JsonObject getJsonData(String defID) throws IOException {
		ImmersiveRailroading.info("Loading stock " + defID);
		ResourceLocation resource = new ResourceLocation(ImmersiveRailroading.MODID, defID);
		
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
}
