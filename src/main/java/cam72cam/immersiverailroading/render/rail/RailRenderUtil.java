package cam72cam.immersiverailroading.render.rail;

import cam72cam.mod.render.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class RailRenderUtil {
	public static void render(RailInfo info, World world, Vec3i pos, boolean renderOverlay) {
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);

		if (renderOverlay) {
			GL11.glPushMatrix();
			Vec3d off = info.placementInfo.placementPosition;
			// TODO Is this needed?
			off = off.subtract(new Vec3d(new Vec3i(off)));
			GL11.glTranslated(- off.x, - off.y, - off.z);
			MinecraftClient.startProfiler("base");
			RailBaseRender.draw(info, world);
			MinecraftClient.endProfiler();
			MinecraftClient.startProfiler("overlay");
			RailBaseOverlayRender.draw(info, world, pos);
			GL11.glPopMatrix();
			MinecraftClient.endProfiler();
		}
		MinecraftClient.startProfiler("rail");
        RailBuilderRender.renderRailBuilder(info, world);
        MinecraftClient.endProfiler();

		light.restore();
	}
}
