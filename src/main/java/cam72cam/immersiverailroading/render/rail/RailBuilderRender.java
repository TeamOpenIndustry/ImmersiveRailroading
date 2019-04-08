package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class RailBuilderRender {

    private static OBJRender baseRailModel;
    private static OBJRender baseRailModelModel;

    static {
        try {
            baseRailModel = new OBJRender(new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m.obj"), 0.05f));
        } catch (Exception e) {
            ImmersiveRailroading.catching(e);
        }
        try {
            baseRailModelModel = new OBJRender(new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m_model.obj"), 0.05f));
        } catch (Exception e) {
            ImmersiveRailroading.catching(e);
        }
    }

    public static OBJRender getModel(Gauge gauge) {
        return gauge.isModel() ? baseRailModel : baseRailModelModel;
    }

    private static DisplayListCache displayLists = new DisplayListCache();
    public static void renderRailBuilder(RailInfo info) {

        OBJRender model = info.settings.gauge.isModel() ? baseRailModel : baseRailModelModel;

        Integer displayList = displayLists.get(info.uniqueID);
        if (displayList == null) {

            if (!ClientProxy.renderCacheLimiter.canRender()) {
                return;
            }

            displayList = ClientProxy.renderCacheLimiter.newList(() -> {

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
                            for (String groupName : model.model.groups())  {
                                if (groupName.contains(baseGroup)) {
                                    groups.add(groupName);
                                }
                            }
                        }


                        model.drawGroups(groups);
                    } else {
                        model.draw();
                    }
                    m.invert();
                    fbm = BufferUtils.createFloatBuffer(16);
                    fbm.put(new float [] {
                            (float)m.m00, (float)m.m01, (float)m.m02, (float)m.m03,
                            (float)m.m10, (float)m.m11, (float)m.m12, (float)m.m13,
                            (float)m.m20, (float)m.m21, (float)m.m22, (float)m.m23,
                            (float)m.m30, (float)m.m31, (float)m.m32, (float)m.m33
                    });
                    fbm.flip();
                    GL11.glMultMatrix(fbm);
                }

            });
            displayLists.put(info.uniqueID, displayList);
        }

        model.bindTexture();
        Minecraft.getMinecraft().mcProfiler.startSection("dl");
        GL11.glCallList(displayList);
        Minecraft.getMinecraft().mcProfiler.endSection();;
        model.restoreTexture();
    }
}
