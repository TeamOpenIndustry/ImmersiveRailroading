package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.immersiverailroading.tile.TileMultiblock;

public class PlateRollerRender implements IMultiblockRender {
	private Model model;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		if (model == null) {
			try {
				this.model = ModelLoader.load(new Identifier("immersiverailroading:models/multiblocks/plate_rolling_machine.obj"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation() - 90, 0, 1, 0);
		state.translate(-2.25, 0, 0.5);
		try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(model).bind(state)) {
			vbo.enqueueOpaque();
		}
	}
}
