package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

public class CustomRender implements IMultiblockRender {
    private OBJModel model;

    @Override
    public void render(TileMultiblock te, RenderState state, float partialTicks) {
        if (model == null) {
            try {
                this.model = new OBJModel(new Identifier("immersiverailroading:models/multiblocks/boiler_rolling_machine.obj"), 0, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        state.translate(0.5, 0, 0.5);
        state.rotate(te.getRotation() - 90, 0, 1, 0);
        state.translate(-3.35, 0, -2.5);

        try (OBJRender.Binding vbo = model.binder().bind(state)) {
            vbo.draw();
        }
    }
}
