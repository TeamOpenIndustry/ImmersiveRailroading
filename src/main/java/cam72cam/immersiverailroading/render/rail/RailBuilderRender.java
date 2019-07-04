package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import javax.vecmath.SingularMatrixException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class RailBuilderRender {

    private static DisplayListCache displayLists = new DisplayListCache();
    public static void renderRailBuilder(RailInfo info) {

        TrackModel model = DefinitionManager.getTrack(info.settings.track, info.settings.gauge.value());
        if (model == null) {
            return;
        }
        OBJRender trackRenderer = StockRenderCache.getTrackRenderer(model);

        Integer displayList = displayLists.get(info.uniqueID);
        if (displayList == null) {

            if (!ClientProxy.renderCacheLimiter.canRender()) {
                return;
            }

            displayList = ClientProxy.renderCacheLimiter.newList(() -> {
                GL11.glPushMatrix();

                for (VecYawPitch piece : info.getBuilder().getRenderData()) {
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

                    m.transpose();
                    FloatBuffer fbm = BufferUtils.createFloatBuffer(16);
                    fbm.put(new float [] {
                            (float)m.m00, (float)m.m01, (float)m.m02, (float)m.m03,
                            (float)m.m10, (float)m.m11, (float)m.m12, (float)m.m13,
                            (float)m.m20, (float)m.m21, (float)m.m22, (float)m.m23,
                            (float)m.m30, (float)m.m31, (float)m.m32, (float)m.m33
                    });
                    fbm.flip();
                    GL11.glMultMatrix(fbm);


                    if (piece.getGroups().size() != 0) {
                        // TODO static
                        ArrayList<String> groups = new ArrayList<String>();
                        for (String baseGroup : piece.getGroups()) {
                            for (String groupName : trackRenderer.model.groups())  {
                                if (groupName.contains(baseGroup)) {
                                    groups.add(groupName);
                                }
                            }
                        }


                        trackRenderer.drawGroups(groups);
                    } else {
                        trackRenderer.draw();
                    }
                    try {
                        m.invert();
                        fbm = BufferUtils.createFloatBuffer(16);
                        fbm.put(new float[]{
                                (float) m.m00, (float) m.m01, (float) m.m02, (float) m.m03,
                                (float) m.m10, (float) m.m11, (float) m.m12, (float) m.m13,
                                (float) m.m20, (float) m.m21, (float) m.m22, (float) m.m23,
                                (float) m.m30, (float) m.m31, (float) m.m32, (float) m.m33
                        });
                        fbm.flip();
                        GL11.glMultMatrix(fbm);
                    } catch (SingularMatrixException e) {
                        // Some weird math happened.  Do this the slow way and reset the matrix
                        GL11.glPopMatrix();
                        GL11.glPushMatrix();
                    }
                }
                GL11.glPopMatrix();

            });
            displayLists.put(info.uniqueID, displayList);
        }

        trackRenderer.bindTexture();
        Minecraft.getMinecraft().mcProfiler.startSection("dl");
        GL11.glCallList(displayList);
        Minecraft.getMinecraft().mcProfiler.endSection();;
        trackRenderer.restoreTexture();
    }
}
