package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class RailRenderUtil {
	public static void render(RailInfo info, World world, Vec3i pos, boolean renderOverlay) {
		try (OpenGL.With light = OpenGL.bool(GL11.GL_LIGHTING, false)) {
			if (renderOverlay) {
				try (OpenGL.With matrix = OpenGL.matrix()) {
					Vec3d off = info.placementInfo.placementPosition;
					// TODO Is this needed?
					off = off.subtract(new Vec3d(new Vec3i(off)));
					GL11.glTranslated(-off.x, -off.y, -off.z);
					MinecraftClient.startProfiler("base");
					RailBaseRender.draw(info, world);
					MinecraftClient.endProfiler();
					MinecraftClient.startProfiler("overlay");
					RailBaseOverlayRender.draw(info, world, pos);
					MinecraftClient.endProfiler();
				}
			}
			MinecraftClient.startProfiler("rail");
			RailBuilderRender.renderRailBuilder(info, world);
			MinecraftClient.endProfiler();
		}
	}
}
