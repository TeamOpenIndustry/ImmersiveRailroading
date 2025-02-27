package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.ModCore;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.RailRollerMultiblock.RailRollerInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class RailRollerRender implements IMultiblockRender {
	private OBJModel model;
	private List<String> input;
	private List<String> output;
	private List<String> rest;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		checkModel();

        input = new ArrayList<>();
        output = new ArrayList<>();
        rest = new ArrayList<>();
        for (String name : model.groups.keySet()) {
            if (name.contains("INPUT_CAST")) {
                input.add(name);
            } else if (name.contains("OUTPUT_RAIL")) {
                output.add(name);
            } else {
                rest.add(name);
            }
        }

		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation() - 90, 0, 1, 0);
		state.translate(-1.5, 0, 0.5);
		try (OBJRender.Binding vbo = model.binder().bind(state)) {
			RailRollerInstance tmb = (RailRollerInstance) te.getMultiblock();
			int progress = tmb.getCraftProgress();

			if (progress != 0) {
				vbo.draw(input, s -> {
					s.translate(0, 0, -(100 - progress) / 10.0);
					s.scale(1, 1, Math.max(0.25, Math.sqrt(progress / 100.0)));
				});
			}
			if (progress != 0) {
				vbo.draw(output, s -> s.translate(0, 0, (progress) / 10.0));
			} else if (tmb.outputFull()) {
				vbo.draw(output);
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
		if(this.model == null){
			try {
				this.model = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/rail_machine.obj"), 0.1f, null);
			} catch (Exception e) {
				ModCore.error(e.toString());
			}
		}
    }
}
