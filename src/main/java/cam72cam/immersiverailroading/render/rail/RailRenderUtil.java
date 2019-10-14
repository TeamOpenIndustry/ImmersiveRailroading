package cam72cam.immersiverailroading.render.rail;

import cam72cam.mod.render.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import org.lwjgl.opengl.GL11;

public class RailRenderUtil {
	public static void render(RailInfo info, boolean renderOverlay) {
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);

		if (renderOverlay) {
			GL11.glPushMatrix();
			Vec3d pos = info.placementInfo.placementPosition;
			pos = pos.subtract(new Vec3d(new Vec3i(pos)));
			GL11.glTranslated(- pos.x, - pos.y, - pos.z);
			MinecraftClient.startProfiler("base");
			RailBaseRender.draw(info);
			MinecraftClient.endProfiler();
			MinecraftClient.startProfiler("overlay");
			RailBaseOverlayRender.draw(info);
			GL11.glPopMatrix();
			MinecraftClient.endProfiler();
		}
		MinecraftClient.startProfiler("rail");
        RailBuilderRender.renderRailBuilder(info);
        MinecraftClient.endProfiler();

		light.restore();
	}
}
