package com.goldenfield192.ire.renderer;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.goldenfield192.ire.IRE;

public class RenderUtils {

    public static final OBJModel wire;

    static {
        try {
            wire = new OBJModel(new Identifier(IRE.MODID,"models/block/wire.obj"),0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void renderStraightLine(RenderState state, double rotXZ, double rotY, double length){
        RenderState thisLine = state.clone();
        double ratio = length/0.02;
        thisLine.scale(1,1,ratio);
        thisLine.rotate(rotY,1,0,0);
        thisLine.rotate(rotXZ,0,1,0);
        try (OBJRender.Binding vbo = wire.binder().bind(thisLine)) {
            vbo.draw();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
