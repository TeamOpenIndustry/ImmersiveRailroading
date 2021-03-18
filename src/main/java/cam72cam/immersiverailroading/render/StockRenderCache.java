package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.entity.StockModel;
import cam72cam.mod.render.obj.OBJRender;

import java.util.HashMap;
import java.util.Map;

public class StockRenderCache {
	private static Map<String, StockModel> render_cache = new HashMap<>();
	private static Map<TrackModel, OBJRender> track_cache = new HashMap<>();

	public static void clearRenderCache() {
		for (StockModel model : render_cache.values()) {
			model.free();
		}
		for (OBJRender model : track_cache.values()) {
			model.free();
		}
		render_cache = new HashMap<>();
		track_cache = new HashMap<>();
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

	public static OBJRender getTrackRenderer(TrackModel model) {
		if (!track_cache.containsKey(model)) {
			track_cache.put(model, new OBJRender(model));
		}
		return track_cache.get(model);
	}
}
