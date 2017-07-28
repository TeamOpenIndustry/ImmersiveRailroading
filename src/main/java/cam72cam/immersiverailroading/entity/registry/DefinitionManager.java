package cam72cam.immersiverailroading.entity.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class DefinitionManager {
	public static final IRenderFactory<EntityRollingStock> RENDER_INSTANCE = new IRenderFactory<EntityRollingStock>() {
		@Override
		public Render<? super EntityRollingStock> createRenderFor(RenderManager manager) {
			return new Render<EntityRollingStock>(manager) {
				@Override
				public boolean shouldRender(EntityRollingStock livingEntity, ICamera camera, double camX, double camY, double camZ) {
					return true;
				}

				@Override
				public void doRender(EntityRollingStock entity, double x, double y, double z, float entityYaw, float partialTicks) {
					entity.render(x, y, z, entityYaw, partialTicks);
				}

				@Override
				protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
					return null;
				}
			};
		}
	};

	private static Map<String, EntityRollingStockDefinition> definitions = new HashMap<String, EntityRollingStockDefinition>();

	public static void initDefinitions() {
		for (String locomotive : Config.locomotives) {
			try {
				String defID = "rolling_stock/locomotives/" + locomotive + ".json";
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
				case "electric":
					loco = new LocomotiveElectricDefinition(defID, data);
					break;
				default:
					ImmersiveRailroading.logger.warn(String.format("Invalid era %s in %s", era, defID));
					continue;
				}
				definitions.put(defID, loco);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		for (String passenger_car : Config.passenger_cars) {
			try {
				String defID = "rolling_stock/passenger/" + passenger_car + ".json";
				JsonObject data = getJsonData(defID);
				definitions.put(defID, new CarPassengerDefinition(defID, data));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		for (String freight_car : Config.freight_cars) {
			try {
				String defID = "rolling_stock/freight/" + freight_car + ".json";
				JsonObject data = getJsonData(defID);
				definitions.put(defID, new CarFreightDefinition(defID, data));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static JsonObject getJsonData(String defID) throws IOException {
		ResourceLocation resource = new ResourceLocation(ImmersiveRailroading.MODID, defID);
		InputStream input = Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();

		JsonParser parser = new JsonParser();
		JsonObject result = parser.parse(new InputStreamReader(input)).getAsJsonObject();
		return result;
	}

	public static EntityRollingStockDefinition getDefinition(String defID) {
		EntityRollingStockDefinition val = definitions.get(defID);
		if (val == null) {
			ImmersiveRailroading.logger.warn("Invalid stock ID: " + defID);
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
