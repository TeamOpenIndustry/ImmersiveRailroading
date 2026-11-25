package cam72cam.immersiverailroading.render.multiblock;

import java.util.*;

import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.immersiverailroading.multiblock.BoilerRollerMultiblock.BoilerRollerInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class BoilerRollerRender implements IMultiblockRender {
	private Model model;
	private List<String> raw;
	private List<String> product;
	private List<String> base;
	private Map<Integer, List<String>> segments;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		if (model == null) {
			try {
				this.model = ModelLoader.load(new Identifier("immersiverailroading:models/multiblocks/boiler_rolling_machine.obj"));
				segments = new HashMap<>();
				product = new ArrayList<>();
				base = new ArrayList<>();
				raw = new ArrayList<>();
				for (String name : model.getGroups().keySet()) {
					if (name.startsWith("RAW_PLATE")) {
						raw.add(name);
					} else if (name.startsWith("PROGRESS")) {
						segments.put(Integer.parseInt(name.substring(9)), Collections.singletonList(name));
					} else if (name.startsWith("FINISHED")) {
						product.add(name);
					} else {
						base.add(name);
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

		try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(model).bind(state)) {
			vbo.enqueueOpaque(base);

			int index = (int) ((100 - tmb.getCraftProgress()) / 6.25);
			index = Math.clamp(index, 0, 16);
			if (tmb.hasOutput()) {
				vbo.enqueueOpaque(product);
			} else {
				boolean hasInput = tmb.hasInput();
				if (hasInput && (index == 0 || tmb.getCraftProgress() == 0)) {
					//Have input and not started crafting/just started crafting
					vbo.enqueueOpaque(raw);
				} else if (hasInput) {
					vbo.enqueueOpaque(segments.get(index));
				}
			}
		}
	}
}
