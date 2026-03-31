package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.VecYPR;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.Light;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.List;

public class RailBuilderRender {
    private static final ExpireableMap<String, VBO> cache = new ExpireableMap<>((k, v) -> v.free());

    public static void renderRailBuilder(RailInfo info, List<VecYPR> renderData, RenderState state, World world, Vec3i basePos) {
        TrackModel model = DefinitionManager.getTrack(info.settings.track, info.settings.gauge.value());
        if (model == null) {
            return;
        }

        VBO cached = cache.get(info.uniqueID);
        if (cached == null) {
            cached = model.getModel(info, renderData);
            cache.put(info.uniqueID, cached);
        }

        MinecraftClient.startProfiler("irTrackModel");
        if (cached instanceof OBJRender) {
            OBJRender objRender = (OBJRender) cached;
            List<OBJRender.PieceRange> ranges = objRender.pieceRanges;

            try (OBJRender.Binding binding = objRender.bind(state, info.settings.type.isTable())) {

                if (ranges.isEmpty()) {//fallback
                    binding.draw();
                } else {
                    Matrix4 worldMatrix = new Matrix4();
                    worldMatrix.translate(
                            basePos.x + 0.5,
                            basePos.y + 0.5,
                            basePos.z + 0.5
                    );

                    for (OBJRender.PieceRange range : ranges) {
                        Vec3d center = Vec3d.ZERO;
                        Matrix4 combined = worldMatrix.copy().multiply(range.localMatrix);
                        center = combined.apply(center);
                        Vec3i pos = new Vec3i(center.x, center.y, center.z);

                        float sky = 1.0f;
                        float block = 1.0f;
                        if (world != null) {
                            sky = world.getSkyLightLevel(pos);
                            block = world.getBlockLightLevel(pos);

                            double extra = 0.0;
                            List<Light.LightInfo> lights = Light.getLightsInRange(center, 8.0);
                            for (Light.LightInfo light : lights) {
                                double distSq = center.distanceToSquared(light.pos);
                                if (distSq < 64.0) {
                                    double dist = Math.sqrt(distSq);
                                    double factor = 1.0 - dist / 8.0;
                                    extra = Math.max(light.level * factor, extra);
                                }
                            }
                            block = (float) Math.min(1.0, Math.max(block ,extra));
                        }
                        binding.drawPiece(range, block, sky);
                    }
                }
            }
        } else {
            try (VBO.Binding vbo = cached.bind(state, info.settings.type.isTable())) {
                vbo.draw();
            }
        }
        MinecraftClient.endProfiler();
    }
}
