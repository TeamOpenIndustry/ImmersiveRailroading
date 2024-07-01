package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.BoilerRollerMultiblock.BoilerRollerInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class BoilerRollerRender implements IMultiblockRender {
	private OBJModel model;
	private List<String> segments;
	private List<String> product;
	private List<String> rest;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		if (model == null) {
			try {
				this.model = new OBJModel(new Identifier("immersiverailroading:models/multiblocks/boiler_rolling_machine.obj"), 0, null);
				segments = new ArrayList<>();
				product = new ArrayList<>();
				rest = new ArrayList<>();
				for (String name : model.groups.keySet()) {
					if (name.contains("SEGMENT_")) {
						segments.add(name);
					} else if (name.contains("FINISHED_PREVIEW")) {
						product.add(name);
					} else {
						rest.add(name);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		BoilerRollerInstance tmb = (BoilerRollerInstance) te.getMultiblock();

		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation() - 90, 0, 1, 0);
		state.translate(-3.5, 0, -2.5);

		try (OBJRender.Binding vbo = model.binder().bind(state)) {
			//TODO better animation
			if (tmb.hasOutput()) {
				vbo.draw(product);
			} else if (tmb.hasInput()) {
				vbo.draw(segments);
			}

			vbo.draw(rest);
		}
	}
}
