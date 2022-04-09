package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.render.opengl.RenderState;

public interface IMultiblockRender {
	public void render(TileMultiblock te, RenderState state, float partialTicks);
}
