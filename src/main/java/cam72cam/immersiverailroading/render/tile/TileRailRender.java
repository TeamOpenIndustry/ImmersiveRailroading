package cam72cam.immersiverailroading.render.tile;

import cam72cam.immersiverailroading.tile.TileRail;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TileRailRender extends TileEntitySpecialRenderer<TileRail> {
	
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}
	

	@Override
	public void render(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		//NOP
	}
}
