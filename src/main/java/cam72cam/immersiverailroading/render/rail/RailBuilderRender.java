package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.List;
import java.util.stream.Collectors;

public class RailBuilderRender {
    private static final ExpireableList<String, VBO> cache = new ExpireableList<String, VBO>() {
        @Override
        public void onRemove(String key, VBO value) {
            value.free();
        }
    };

    public static void renderRailBuilder(RailInfo info, World world, RenderState state) {
        TrackModel model = DefinitionManager.getTrack(info.settings.track, info.settings.gauge.value());
        if (model == null) {
            return;
        }

        VBO cached = cache.get(info.uniqueID);
        if (cached == null) {
            OBJRender.Builder builder = model.binder().builder();

            for (VecYawPitch piece : info.getBuilder(world).getRenderData()) {
                Matrix4 m = new Matrix4();
                //m.rotate(Math.toRadians(info.placementInfo.yaw), 0, 1, 0);
                m.translate(piece.x, piece.y, piece.z);
                m.rotate(Math.toRadians(piece.getYaw()), 0, 1, 0);
                m.rotate(Math.toRadians(piece.getPitch()), 1, 0, 0);
                m.rotate(Math.toRadians(-90), 0, 1, 0);

                if (piece.getLength() != -1) {
                    m.scale(piece.getLength() / info.settings.gauge.scale(), 1, 1);
                }
                double scale = info.settings.gauge.scale();
                m.scale(scale, scale, scale);

                if (piece.getGroups().size() != 0) {
                    List<String> groups = model.groups().stream()
                            .filter(group -> piece.getGroups().stream().anyMatch(group::contains))
                            .collect(Collectors.toList());
                    builder.draw(groups, m);
                } else {
                    builder.draw(m);
                }
            }
            cached = builder.build();
            cache.put(info.uniqueID, cached);
        }

        MinecraftClient.startProfiler("irTrackModel");
        try (VBO.Binding vbo = cached.bind(state)) {
            vbo.draw();
        }
        MinecraftClient.endProfiler();
    }
}
