package cam72cam.immersiverailroading.render;

import java.util.HashMap;
import java.util.Map;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.entity.StockModel;

public class StockRenderCache {
	private static Map<String, StockModel> render_cache = new HashMap<String, StockModel>();
	private static boolean isCachePrimed = false;

	public static void clearRenderCache() {
		for (StockModel model : render_cache.values()) {
			model.freeGL();
		}
		render_cache = new HashMap<String, StockModel>();
		isCachePrimed = false;
	}

	private static void tryPrimeRenderCache() {
		if(isCachePrimed) {
			return;
		}
		isCachePrimed = true;
		
		for (String def : DefinitionManager.getDefinitionNames()) {
			ImmersiveRailroading.info("Priming Render Cache: %s", def);;
			StockModel renderer = getRender(def);
			if (ConfigGraphics.enableItemRenderPriming) {
				renderer.bindTexture();
				renderer.draw();
				renderer.restoreTexture();
				ClientProxy.renderCacheLimiter.reset();
			}
		}
	}

	public static StockModel getRender(String defID) {
		if (!render_cache.containsKey(defID)) {
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def != null) {
				render_cache.put(defID, new StockModel(def.getModel()));
			}
		}
		return render_cache.get(defID);
	}

	public static void tryPrime() {
		tryPrimeRenderCache();
	}
}
