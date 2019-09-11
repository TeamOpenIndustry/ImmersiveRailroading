package cam72cam.immersiverailroading.render.tile;

import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GlobalRender;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

public class MultiPreviewRender {
    private static ExpireableList<Long, TileRailPreview> previews = new ExpireableList<>();

    static {
        GlobalRender.registerRender(MultiPreviewRender::render);
    }

    private static void render(float partialTicks) {
        GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
        GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
        if (GLContext.getCapabilities().OpenGL14) {
            GL14.glBlendColor(1, 1, 1, 0.7f);
        }
        for (TileRailPreview preview : previews.values()) {
            for (BuilderBase builder : ((IIterableTrack) preview.getRailRenderInfo().getBuilder(preview.pos)).getSubBuilders()) {
                RailInfo info = builder.info;
                Vec3d placementPosition = info.placementInfo.placementPosition;

                if (GlobalRender.isInRenderDistance(placementPosition)) {
                    placementPosition = placementPosition.subtract(GlobalRender.getCameraPos(partialTicks));
                    GL11.glPushMatrix();
                    {
                        GL11.glTranslated(placementPosition.x, placementPosition.y, placementPosition.z);

                        RailRenderUtil.render(info, true);
                    }
                    GL11.glPopMatrix();
                }
            }
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (GLContext.getCapabilities().OpenGL14) {
            GL14.glBlendColor(1, 1, 1, 1f);
        }
        blend.restore();
    }

    public static void add(TileRailPreview preview) {
        previews.put(preview.pos.toLong(), preview);
    }
}
