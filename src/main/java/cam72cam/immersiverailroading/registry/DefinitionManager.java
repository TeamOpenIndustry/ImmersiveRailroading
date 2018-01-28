package cam72cam.immersiverailroading.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class DefinitionManager {

	private static Map<String, EntityRollingStockDefinition> definitions;

	public static void initDefinitions() throws IOException {
		definitions = new LinkedHashMap<String, EntityRollingStockDefinition>();

		ResourceLocation resource = new ResourceLocation(ImmersiveRailroading.MODID, "rolling_stock/stock.json");
		
		List<InputStream> inputs = ImmersiveRailroading.proxy.getResourceStreamAll(resource);
		for (InputStream input : inputs) {
		
			JsonParser parser = new JsonParser();
			JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
			
			for (JsonElement locomotive : stock.get("locomotives").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/locomotives/" + locomotive .getAsString()+ ".json";
					JsonObject data = getJsonData(defID);
					String era = data.get("era").getAsString();
					LocomotiveDefinition loco = null;
					switch (era) {
					case "steam":
						loco = new LocomotiveSteamDefinition(defID, data);
						break;
					case "diesel":
						loco = new LocomotiveDieselDefinition(defID, data);
						break;
					default:
						ImmersiveRailroading.warn("Invalid era %s in %s", era, defID);
						continue;
					}
					definitions.put(defID, loco);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement tender : stock.get("tender").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/tender/" + tender.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new TenderDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement passenger_car : stock.get("passenger").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/passenger/" + passenger_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarPassengerDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement freight_car : stock.get("freight").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/freight/" + freight_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarFreightDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement tank_car : stock.get("tank").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/tank/" + tank_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarTankDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement hand_car : stock.get("hand_car").getAsJsonArray()) {
				try {
					String defID = "rolling_stock/hand_car/" + hand_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new HandCarDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	private static JsonObject getJsonData(String defID) throws IOException {
		ImmersiveRailroading.info("Loading stock " + defID);
		ResourceLocation resource = new ResourceLocation(ImmersiveRailroading.MODID, defID);
		
		InputStream input = ImmersiveRailroading.proxy.getResourceStream(resource);

		JsonParser parser = new JsonParser();
		JsonObject result = parser.parse(new InputStreamReader(input)).getAsJsonObject();
		return result;
	}

	public static EntityRollingStockDefinition getDefinition(String defID) {
		EntityRollingStockDefinition val = definitions.get(defID);
		if (val == null) {
			ImmersiveRailroading.warn("Invalid stock ID: " + defID);
		}
		return val;
	}

	public static Collection<EntityRollingStockDefinition> getDefinitions() {
		return definitions.values();
	}

	public static Set<String> getDefinitionNames() {
		return definitions.keySet();
	}
}
