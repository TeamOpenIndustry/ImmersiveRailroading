package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class RailRenderUtil {
	public static void render(RailInfo info, boolean renderOverlay) {
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);

		if (renderOverlay) {

			// Bind block textures to current context
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

			GL11.glPushMatrix();
			Vec3d pos = info.placementInfo.placementPosition.internal;
			pos = pos.subtract(new Vec3d(new BlockPos(pos)));
			GL11.glTranslated(- pos.x, - pos.y, - pos.z);
			Minecraft.getMinecraft().mcProfiler.startSection("base");
			RailBaseRender.draw(info);
			Minecraft.getMinecraft().mcProfiler.endStartSection("overlay");
			RailBaseOverlayRender.draw(info);
			GL11.glPopMatrix();
			Minecraft.getMinecraft().mcProfiler.endSection();
		}

		Minecraft.getMinecraft().mcProfiler.startSection("rail");
        RailBuilderRender.renderRailBuilder(info);
		Minecraft.getMinecraft().mcProfiler.endSection();

		light.restore();
	}
}
