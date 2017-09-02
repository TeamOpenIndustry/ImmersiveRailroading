package cam72cam.immersiverailroading.render.rail;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TileRailRender extends TileEntitySpecialRenderer<TileRail> {
	
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}
	

	@Override
	public void render(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!te.isVisible()) {
			return;
		}
		RailInfo info = te.getRailRenderInfo();
		GL11.glPushMatrix();
		{
			// Move to specified position
			GlStateManager.translate(x, y, z);
			
			RailRenderUtil.render(info, false);
		}
		GL11.glPopMatrix();
	}
}
