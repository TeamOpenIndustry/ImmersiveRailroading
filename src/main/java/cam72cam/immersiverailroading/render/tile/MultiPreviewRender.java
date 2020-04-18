package cam72cam.immersiverailroading.render.tile;

import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.GLTransparencyHelper;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

public class MultiPreviewRender {
    private static ExpireableList<Pair<World, Vec3i>, TileRailPreview> previews = new ExpireableList<>();

    static {
        GlobalRender.registerRender(MultiPreviewRender::render);
    }

    private static void render(float partialTicks) {
        GLTransparencyHelper transparency = new GLTransparencyHelper(1,1,1, 0.7f);

        for (TileRailPreview preview : previews.values()) {
            for (BuilderBase builder : ((IIterableTrack) preview.getRailRenderInfo().getBuilder(preview.world, preview.pos)).getSubBuilders()) {
                RailInfo info = builder.info;
                Vec3d placementPosition = info.placementInfo.placementPosition.add(preview.pos);

                if (GlobalRender.isInRenderDistance(placementPosition)) {
                    placementPosition = placementPosition.subtract(GlobalRender.getCameraPos(partialTicks));
                    GL11.glPushMatrix();
                    {
                        GL11.glTranslated(placementPosition.x, placementPosition.y, placementPosition.z);

                        RailRenderUtil.render(info, preview.world, preview.pos, true);
                    }
                    GL11.glPopMatrix();
                }
            }
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        transparency.restore();
    }

    public static void add(TileRailPreview preview) {
        previews.put(Pair.of(preview.world, preview.pos), preview);
    }

    public static void remove(World world, Vec3i removed) {
        previews.put(Pair.of(world, removed), null);
    }
}
