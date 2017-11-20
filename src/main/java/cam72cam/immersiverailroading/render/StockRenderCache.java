package cam72cam.immersiverailroading.render;

import java.util.HashMap;
import java.util.Map;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

public class StockRenderCache {
	private static Map<String, StockModel> render_cache = new HashMap<String, StockModel>();
	public static void clearRenderCache() {
		render_cache = new HashMap<String, StockModel>(); 
		primeCache();
	}
	
	public static void primeCache() {
		for (String def : DefinitionManager.getDefinitionNames()) {
			ImmersiveRailroading.info("Priming Render Cache: %s", def);;
			getRender(def);
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

}
