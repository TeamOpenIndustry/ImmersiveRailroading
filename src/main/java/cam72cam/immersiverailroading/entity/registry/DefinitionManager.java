package cam72cam.immersiverailroading.entity.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
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
	
	private static Map<String, IDefinitionRollingStock> definitions = new HashMap<String, IDefinitionRollingStock>();
	
	public static void initDefinitions() {
		for(String locomotive : Config.locomotives) {
			try {
				String defID = "rolling_stock/locomotives/" + locomotive + ".json";
				definitions.put(defID, new RegisteredSteamLocomotive(defID));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static List<ResourceLocation> getTextures() {
		List<ResourceLocation> result = new ArrayList<ResourceLocation>();
		for(IDefinitionRollingStock stock : definitions.values()) {
			result.addAll(stock.getTextures());
		}
		return result;
	}

	public static IDefinitionRollingStock getDefinition(String defID) {
		return definitions.get(defID);
	}

	public static Collection<IDefinitionRollingStock> getDefinitions() {
		return definitions.values();
	}
}
