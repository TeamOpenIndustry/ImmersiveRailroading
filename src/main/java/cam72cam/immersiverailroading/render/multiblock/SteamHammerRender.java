package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;

import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.SteamHammerMultiblock.SteamHammerInstance;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class SteamHammerRender implements IMultiblockRender {
	private OBJRender renderer;
	private ArrayList<String> hammer;
	private ArrayList<String> rest;

	@Override
	public void render(TileMultiblock te, float partialTicks) {
		if (renderer == null) {
			try {
				this.renderer = new OBJRender(new OBJModel(new Identifier("immersiverailroading:models/multiblocks/steam_hammer.obj"), -0.1f, null));
				this.hammer = new ArrayList<>();
				this.rest = new ArrayList<>();
				for (String group : renderer.model.groups()) {
					if (group.contains("Hammer")) {
						hammer.add(group);
					} else {
						rest.add(group);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try (OpenGL.With matrix = OpenGL.matrix(); OpenGL.With tex = renderer.bindTexture()) {
			SteamHammerInstance mb = (SteamHammerInstance) te.getMultiblock();

			//GL11.glScaled(2, 2, 2);
			GL11.glTranslated(0.5, 0, 0.5);
			GL11.glRotated(te.getRotation(), 0, 1, 0);
			renderer.drawGroups(rest);
			if (mb != null && mb.hasPower()) {
				if (te.getCraftProgress() != 0) {
					GL11.glTranslated(0, -(Math.abs((te.getRenderTicks() + partialTicks) % 10 - 5)) / 4f, 0);
				} else {
					GL11.glTranslated(0, -(Math.abs((te.getRenderTicks() + partialTicks) % 30 - 15)) / 14f, 0);
				}
			}
			renderer.drawGroups(hammer);
		}
	}
}
