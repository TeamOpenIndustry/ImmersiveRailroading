package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.ModCore;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class CastingRender implements IMultiblockRender {
	private OBJModel model;
	private List<String> flowing_steel;
	private List<String> steel_level;
	private List<String> rest;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
        checkModel();

        flowing_steel = new ArrayList<>();
        steel_level = new ArrayList<>();
        rest = new ArrayList<>();
        for (String name : model.groups.keySet()) {
            if (name.contains("FLOWING_STEEL")) {
                flowing_steel.add(name);
            } else if (name.contains("STEEL_LEVEL")) {
                steel_level.add(name);
            } else {
                rest.add(name);
            }
        }

		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation() - 90, 0, 1, 0);
		state.translate(-2.5, -3, 6.5);
		try (OBJRender.Binding vbo = model.binder().bind(state)) {
			CastingInstance tmb = (CastingInstance) te.getMultiblock();
			if (tmb.isPouring()) {
				vbo.draw(flowing_steel);
			}
			double steelLevel = tmb.getSteelLevel() * 4.5;
			if (steelLevel != 0) {
				vbo.draw(steel_level, s -> s.translate(0, steelLevel, 0));
			}
			vbo.draw(rest);
		}
	}

    public OBJModel getModel() {
        checkModel();
        return model;
    }

    @Override
    public void checkModel() {
        if (model == null) {
            try {
                this.model = new OBJModel(new Identifier("immersiverailroading:models/multiblocks/casting_machine.obj"), 0, null);
            } catch (Exception e) {
                ModCore.error(e.toString());
            }
        }
    }
}
