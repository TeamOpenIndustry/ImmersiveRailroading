package cam72cam.immersiverailroading.render;

import java.util.HashMap;
import java.util.Map;

import cam72cam.immersiverailroading.multiblock.PlateRollerMultiblock;
import cam72cam.immersiverailroading.multiblock.RailRollerMultiblock;
import cam72cam.immersiverailroading.multiblock.SteamHammerMultiblock;
import cam72cam.immersiverailroading.render.multiblock.IMultiblockRender;
import cam72cam.immersiverailroading.render.multiblock.PlateRollerRender;
import cam72cam.immersiverailroading.render.multiblock.RailRollerRender;
import cam72cam.immersiverailroading.render.multiblock.SteamHammerRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TileMultiblockRender extends TileEntitySpecialRenderer<TileMultiblock> {
	
	public final Map<String, IMultiblockRender> renderers = new HashMap<String, IMultiblockRender>();
	
	public TileMultiblockRender() {
		renderers.put(SteamHammerMultiblock.NAME, new SteamHammerRender());
		renderers.put(PlateRollerMultiblock.NAME, new PlateRollerRender());
		renderers.put(RailRollerMultiblock.NAME, new RailRollerRender());
	}
	
	@Override
	public boolean isGlobalRenderer(TileMultiblock te) {
		return true;
	}
	
	@Override
	public void render(TileMultiblock te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te.isLoaded() && te.isRender()) {
			IMultiblockRender renderer = renderers.get(te.getName());
			if (renderer != null) {
				renderer.render(te, x, y, z, partialTicks);
			}
		}
	}
}
