package cam72cam.immersiverailroading.render;

import java.util.HashMap;
import java.util.Map;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.TrackDefinition;
import cam72cam.immersiverailroading.render.entity.StockModel;
import cam72cam.mod.gui.Progress;
import cam72cam.mod.render.obj.OBJRender;

public class StockRenderCache {
	private static Map<String, StockModel> render_cache = new HashMap<>();
	private static Map<TrackModel, OBJRender> track_cache = new HashMap<>();
	private static boolean isCachePrimed = false;

	public static void clearRenderCache() {
		for (StockModel model : render_cache.values()) {
			model.freeGL();
		}
		for (OBJRender model : track_cache.values()) {
			model.freeGL();
		}
		render_cache = new HashMap<>();
		track_cache = new HashMap<>();
		isCachePrimed = false;
	}

	private static void tryPrimeRenderCache() {
		if(isCachePrimed) {
			return;
		}
		isCachePrimed = true;

		Progress.pop();

		Progress.Bar bar = Progress.push("Uploading IR Textures", DefinitionManager.getDefinitionNames().size());
		
		for (String def : DefinitionManager.getDefinitionNames()) {
			bar.step(DefinitionManager.getDefinition(def).name());
			ImmersiveRailroading.info(def);
			StockModel renderer = getRender(def);
            renderer.bindTexture();
            renderer.draw();
            renderer.restoreTexture();
            ClientProxy.renderCacheLimiter.reset();
		}

		Progress.pop(bar);

		for (TrackDefinition def : DefinitionManager.getTracks()) {
			ImmersiveRailroading.info(def.trackID);
			for (TrackModel model : def.models) {
				getTrackRenderer(model).bindTexture();
				getTrackRenderer(model).draw();
				getTrackRenderer(model).restoreTexture();
				ClientProxy.renderCacheLimiter.reset();
			}
		}
	}

	public static StockModel getRender(String defID) {
		if (!render_cache.containsKey(defID)) {
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def != null) {
				render_cache.put(defID, new StockModel(def.getModel(), def.textureNames.keySet()));
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

	public static void tryPrime() {
		tryPrimeRenderCache();
	}
}
