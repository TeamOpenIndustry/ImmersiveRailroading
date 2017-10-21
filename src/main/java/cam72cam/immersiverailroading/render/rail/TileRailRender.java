package cam72cam.immersiverailroading.render.rail;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class TileRailRender extends TileRailBaseRender<TileRail> {
	
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}
	

	@Override
	public void render(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		super.render(te, x, y, z, partialTicks, destroyStage, alpha);
		
		RailInfo info = te.getRailRenderInfo();
		if (info == null) {
			// Still loading...
			return;
		}
		Minecraft.getMinecraft().mcProfiler.startSection("tile_rail");
		GL11.glPushMatrix();
		{
			// Move to specified position
			GlStateManager.translate(x, y, z);
			
			RailRenderUtil.render(info, false);
		}
		GL11.glPopMatrix();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}
}
