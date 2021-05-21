package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.RailRollerMultiblock.RailRollerInstance;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class RailRollerRender implements IMultiblockRender {
	private OBJRender renderer;
	private List<String> input;
	private List<String> output;
	private List<String> rest;

	@Override
	public void render(TileMultiblock te, float partialTicks) {
		if (renderer == null) {
			try {
				this.renderer = new OBJRender(new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/rail_machine.obj"), 0.1f, null));
				input = new ArrayList<>();
				output = new ArrayList<>();
				rest = new ArrayList<>();
				for (String name : renderer.model.groups.keySet()) {
					if (name.contains("INPUT_CAST")) {
						input.add(name);
					} else if (name.contains("OUTPUT_RAIL")) {
						output.add(name);
					} else {
						rest.add(name);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try (OpenGL.With matrix = OpenGL.matrix(); OpenGL.With tex = renderer.bindTexture()) {
			GL11.glTranslated(0.5, 0, 0.5);
			GL11.glRotated(te.getRotation() - 90, 0, 1, 0);
			GL11.glTranslated(-1.5, 0, 0.5);

			RailRollerInstance tmb = (RailRollerInstance) te.getMultiblock();
			int progress = tmb.getCraftProgress();

			if (progress != 0) {
				try (OpenGL.With m = OpenGL.matrix()) {
					GL11.glTranslated(0, 0, -(100 - progress) / 10.0);
					GL11.glScaled(1, 1, Math.max(0.25, Math.sqrt(progress / 100.0)));
					renderer.drawGroups(input);
				}
			}
			try (OpenGL.With m = OpenGL.matrix()) {
				if (progress != 0) {
					GL11.glTranslated(0, 0, (progress) / 10.0);
					renderer.drawGroups(output);
				} else if (tmb.outputFull()) {
					renderer.drawGroups(output);
				}
			}

			renderer.drawGroups(rest);
		}
	}
}
