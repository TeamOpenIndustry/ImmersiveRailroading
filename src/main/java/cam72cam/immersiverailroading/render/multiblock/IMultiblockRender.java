package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.tile.TileMultiblock;

public interface IMultiblockRender {
	public void render(TileMultiblock te, double x, double y, double z, float partialTicks);
}
