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

public class DefinitionManager {

	private static Map<String, EntityRollingStockDefinition> definitions;

	public static void initDefinitions() throws IOException {
		definitions = new LinkedHashMap<String, EntityRollingStockDefinition>();
		
		List<String> blacklist = new ArrayList<String>();
		
		ResourceLocation blacklist_json = new ResourceLocation(ImmersiveRailroading.MODID, "rolling_stock/blacklist.json");
		
		List<InputStream> inputs = ImmersiveRailroading.proxy.getResourceStreamAll(blacklist_json);
		for (InputStream input : inputs) {
			JsonParser parser = new JsonParser();
			JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
			input.close();
			
			for (JsonElement locomotive : stock.get("locomotives").getAsJsonArray()) {
				blacklist.add(locomotive.getAsString());
			}
			for (JsonElement tender : stock.get("tender").getAsJsonArray()) {
				blacklist.add(tender.getAsString());
			}
			for (JsonElement passenger_car : stock.get("passenger").getAsJsonArray()) {
				blacklist.add(passenger_car.getAsString());
			}
			for (JsonElement freight_car : stock.get("freight").getAsJsonArray()) {
				blacklist.add(freight_car.getAsString());
			}
			for (JsonElement tank_car : stock.get("tank").getAsJsonArray()) {
				blacklist.add(tank_car.getAsString());
			}
			for (JsonElement hand_car : stock.get("hand_car").getAsJsonArray()) {
				blacklist.add(hand_car.getAsString());
			}
		}
		

		ResourceLocation stock_json = new ResourceLocation(ImmersiveRailroading.MODID, "rolling_stock/stock.json");
		
		inputs = ImmersiveRailroading.proxy.getResourceStreamAll(stock_json);
		for (InputStream input : inputs) {
		
			JsonParser parser = new JsonParser();
			JsonObject stock = parser.parse(new InputStreamReader(input)).getAsJsonObject();
			input.close();
			
			for (JsonElement locomotive : stock.get("locomotives").getAsJsonArray()) {
				if (blacklist.contains(locomotive.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", locomotive.getAsString());
					continue;
				}
				
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
				if (blacklist.contains(tender.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", tender.getAsString());
					continue;
				}
				try {
					String defID = "rolling_stock/tender/" + tender.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new TenderDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement passenger_car : stock.get("passenger").getAsJsonArray()) {
				if (blacklist.contains(passenger_car.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", passenger_car.getAsString());
					continue;
				}
				try {
					String defID = "rolling_stock/passenger/" + passenger_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarPassengerDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement freight_car : stock.get("freight").getAsJsonArray()) {
				if (blacklist.contains(freight_car.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", freight_car.getAsString());
					continue;
				}
				try {
					String defID = "rolling_stock/freight/" + freight_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarFreightDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement tank_car : stock.get("tank").getAsJsonArray()) {
				if (blacklist.contains(tank_car.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", tank_car.getAsString());
					continue;
				}
				try {
					String defID = "rolling_stock/tank/" + tank_car.getAsString() + ".json";
					JsonObject data = getJsonData(defID);
					definitions.put(defID, new CarTankDefinition(defID, data));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (JsonElement hand_car : stock.get("hand_car").getAsJsonArray()) {
				if (blacklist.contains(hand_car.getAsString())) {
					ImmersiveRailroading.info("Skipping blacklisted %s", hand_car.getAsString());
					continue;
				}
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
