package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.render.VBO;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.obj.OBJVBO;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StockRenderCache {
	private static Map<String, OBJRender> render_cache = new HashMap<>();
	private static Map<String, VBO> vbo_cache = new HashMap<>();
	private static Map<TrackModel, OBJRender> track_cache = new HashMap<>();

	public static void clearRenderCache() {
		for (OBJRender model : render_cache.values()) {
			model.free();
		}
		for (OBJRender model : track_cache.values()) {
			model.free();
		}
		render_cache = new HashMap<>();
		track_cache = new HashMap<>();
		vbo_cache = new HashMap<>();
	}

	public static OBJRender getRender(String defID) {
		if (!render_cache.containsKey(defID)) {
			EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
			if (def != null) {
				render_cache.put(defID, new OBJRender(def.getModel(), ConfigGraphics.textureCacheSeconds));
			}
		}
		return render_cache.get(defID);
	}

	public static VBO getVBO(String defID) {
		if (!vbo_cache.containsKey(defID)) {
			OBJRender renderer = getRender(defID);
			if (renderer == null) {
				return null;
			}
			OBJVBO.Builder builder = renderer.getVBO().subModel();
			builder.draw(renderer.model.groups.keySet().stream().filter(x -> !ModelComponentType.isParticle(x)).collect(Collectors.toList()));
			vbo_cache.put(defID, builder.build());
		}
		return vbo_cache.get(defID);
	}

	public static OBJRender getTrackRenderer(TrackModel model) {
		if (!track_cache.containsKey(model)) {
			track_cache.put(model, new OBJRender(model));
		}
		return track_cache.get(model);
	}
}
