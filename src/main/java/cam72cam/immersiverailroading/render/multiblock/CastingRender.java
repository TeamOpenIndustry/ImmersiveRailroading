package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class CastingRender implements IMultiblockRender {
	private Model model;
	private List<String> flowing_steel;
	private List<String> steel_level;
	private List<String> rest;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		if (model == null) {
			try {
				this.model = ModelLoader.load(new Identifier("immersiverailroading:models/multiblocks/casting_machine.obj"));
				flowing_steel = new ArrayList<>();
				steel_level = new ArrayList<>();
				rest = new ArrayList<>();
				for (String name : model.getGroups().keySet()) {
					if (name.contains("FLOWING_STEEL")) {
						flowing_steel.add(name);
					} else if (name.contains("STEEL_LEVEL")) {
						steel_level.add(name);
					} else {
						rest.add(name);
					}
				}
			} catch (Exception e) {
				ModCore.catching(e);
			}
		}

		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation() - 90, 0, 1, 0);
		state.translate(-2.5, -3, 6.5);
		try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(model).bind(state)) {
			CastingInstance tmb = (CastingInstance) te.getMultiblock();
			if (tmb.isPouring()) {
				vbo.enqueueOpaque(flowing_steel);
			}
			double steelLevel = tmb.getSteelLevel() * 4.5;
			if (steelLevel != 0) {
				vbo.enqueueOpaque(steel_level, s -> s.translate(0, steelLevel, 0));
			}
			vbo.enqueueOpaque(rest);
		}
	}
}
