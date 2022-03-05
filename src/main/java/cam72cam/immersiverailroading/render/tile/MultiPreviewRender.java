package cam72cam.immersiverailroading.render.tile;

import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

public class MultiPreviewRender {
    private static ExpireableList<Pair<World, Vec3i>, TileRailPreview> previews = new ExpireableList<>();

    static {
        GlobalRender.registerRender(MultiPreviewRender::render);
    }

    private static void render(RenderState state, float partialTicks) {
        //TODO BORK BORK BORK try (OpenGL.With transparency = OpenGL.transparency(1,1,1, 0.7f)) {
        for (TileRailPreview preview : previews.values()) {
            for (BuilderBase builder : ((IIterableTrack) preview.getRailRenderInfo().getBuilder(preview.getWorld(), preview.isAboveRails() ? preview.getPos().down() :preview.getPos())).getSubBuilders()) {
                RailInfo info = builder.info;
                Vec3d placementPosition = info.placementInfo.placementPosition.add(builder.pos);

                if (GlobalRender.getCameraPos(partialTicks).distanceTo(placementPosition) < GlobalRender.getRenderDistance() + 50) {
                    placementPosition = placementPosition.subtract(GlobalRender.getCameraPos(partialTicks));
                    state.translate(placementPosition);
                    RailRenderUtil.render(info, preview.getWorld(), builder.pos, true, state);
                }
            }
        }
    }

    public static void add(TileRailPreview preview) {
        previews.put(Pair.of(preview.getWorld(), preview.getPos()), preview);
    }

    public static void remove(World world, Vec3i removed) {
        previews.put(Pair.of(world, removed), null);
    }
}
