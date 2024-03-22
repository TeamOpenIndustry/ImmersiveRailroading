package com.goldenfield192.ire.renderer;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.goldenfield192.ire.IRE;

public class SimpleBlockRenderer {

    static OBJModel model;
    static {
        try {
            model = new OBJModel(new Identifier(IRE.MODID,"models/block/guanmu.obj"),0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StandardModel render(BlockEntity be) {
        return new StandardModel().addCustom((state, partialTicks) -> BlockRenderer(be,state));
    }

    private static void BlockRenderer(BlockEntity be, RenderState state){
        state.smooth_shading(true);
        RenderState baseState = state.clone();
        baseState.scale(0.5,0.5,0.5);
        baseState.translate(0.5,0.5,0.5);
        try (OBJRender.Binding vbo = model.binder().bind(baseState)) {
            vbo.draw();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
