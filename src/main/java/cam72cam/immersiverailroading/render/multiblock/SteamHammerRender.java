package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;

import cam72cam.mod.ModCore;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.SteamHammerMultiblock.SteamHammerInstance;
import cam72cam.immersiverailroading.tile.TileMultiblock;

public class SteamHammerRender implements IMultiblockRender {
	private OBJModel model;
	private ArrayList<String> hammer;
	private ArrayList<String> rest;

	@Override
	public void render(TileMultiblock te, RenderState state, float partialTicks) {
		checkModel();

        this.hammer = new ArrayList<>();
        this.rest = new ArrayList<>();
        for (String group : model.groups()) {
            if (group.contains("Hammer")) {
                hammer.add(group);
            } else {
                rest.add(group);
            }
        }

		SteamHammerInstance mb = (SteamHammerInstance) te.getMultiblock();

		//state.scale(2, 2, 2);
		state.translate(0.5, 0, 0.5);
		state.rotate(te.getRotation(), 0, 1, 0);
		try (OBJRender.Binding vbo = model.binder().bind(state)) {
			vbo.draw(rest);
			double dist;
			if (mb != null && mb.hasPower()) {
				if (te.getCraftProgress() != 0) {
					dist = -(Math.abs((te.getRenderTicks() + partialTicks) % 10 - 5)) / 4f;
				} else {
					dist = -(Math.abs((te.getRenderTicks() + partialTicks) % 30 - 15)) / 14f;
				}
			} else {
				dist = 0;
			}
			vbo.draw(hammer, s -> s.translate(0, dist, 0));
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
                this.model = new OBJModel(new Identifier("immersiverailroading:models/multiblocks/steam_hammer.obj"), -0.1f, null);
            } catch (Exception e) {
				ModCore.error(e.toString());
            }
        }
    }
}
