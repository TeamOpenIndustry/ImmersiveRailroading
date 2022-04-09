package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;

public class RailRenderUtil {
	public static void render(RailInfo info, World world, Vec3i pos, boolean renderOverlay, RenderState state) {
		state.lighting(false);

		MinecraftClient.startProfiler("rail");
		RailBuilderRender.renderRailBuilder(info, world, state);
		MinecraftClient.endProfiler();

		if (renderOverlay) {
			Vec3d off = info.placementInfo.placementPosition;
			// TODO Is this needed?
			off = off.subtract(new Vec3d(new Vec3i(off)));
			state.translate(-off.x, -off.y, -off.z);

			MinecraftClient.startProfiler("base");
			RailBaseRender.draw(info, world, state);
			MinecraftClient.endProfiler();

			MinecraftClient.startProfiler("overlay");
			RailBaseOverlayRender.draw(info, world, pos, state);
			MinecraftClient.endProfiler();
		}
	}
}
