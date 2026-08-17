package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.mod.MinecraftClient;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;

import java.util.List;

public class RailBuilderRender {
    private static final ExpireableMap<String, Model> cache = new ExpireableMap<>((k, v) -> v.free());

    public static void renderRailBuilder(RailInfo info, List<VecYPR> renderData, RenderState state) {
        TrackModel track = DefinitionManager.getTrack(info.settings.track, info.settings.gauge.value());
        if (track == null) {
            return;
        }

        Model cached = cache.get(info.uniqueID);
        if (cached == null) {
            cached = track.getModel(info, renderData);
            cache.put(info.uniqueID, cached);
        }

        MinecraftClient.startProfiler("irTrackModel");
        try (ModelRenderer.Binding binding =
                     ModelRenderer.getRendererFor(track.model).bind(state, info.settings.type.isTable())) {
            binding.enqueueOpaque();
        }
        MinecraftClient.endProfiler();
    }
}
