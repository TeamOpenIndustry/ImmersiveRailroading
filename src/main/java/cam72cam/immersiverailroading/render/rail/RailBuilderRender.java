package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.util.collection.ExpireableMap;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.MinecraftClient;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;

import java.util.List;

public class RailBuilderRender {
    private static final ExpireableMap<String, VBO> cache = new ExpireableMap<>((k, v) -> v.free());

    public static void renderRailBuilder(RailInfo info, List<VecYPR> renderData, RenderState state) {
        TrackModel track = DefinitionManager.getTrack(info.settings.track, info.settings.gauge.value());
        if (track == null) {
            return;
        }

        VBO cached = cache.get(info.uniqueID);
        if (cached == null) {
            cached = track.getModel(info, renderData);
            cache.put(info.uniqueID, cached);
        }

        MinecraftClient.startProfiler("irTrackModel");
        try (VBO.Binding binding = cached.bind(state, info.settings.type.isTable())) {
            binding.draw();
        }
        MinecraftClient.endProfiler();
    }
}
