package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;

import java.util.*;

public class CustomMultiblockRender implements IMultiblockRender {
    private static final Map<String, MultiblockDefinition> refer;
    private static final Vec3d north = new Vec3d(1,0,1);
    private static final Vec3d west = new Vec3d(1,0,0);
    private static final Vec3d east = new Vec3d(0,0,1);

    static {
        refer = new HashMap<>();
    }

    public static void addDef(MultiblockDefinition def){
        refer.put(def.name, def);
    }

    @Override
    public void render(TileMultiblock te, RenderState state, float partialTicks) {
        MultiblockModel model = refer.get(te.getName()).model;
        if(model != null){
            //translate back
            Vec3i t = refer.get(te.getName()).center.rotate(te.geFacing());
            state.translate(-t.x, -t.y, -t.z);
//            state.translate(0,0, t.);
            int temp = (int) (te.getRotation() - 90);
            state.rotate(temp, 0, 1, 0);
            switch (temp){
                case 0:
                    state.translate(north);
                    break;
                case 90:
                    state.translate(east);
                    break;
                case -90:
                    state.translate(west);
            }
            try (OBJRender.Binding vbo = model.binder().bind(state)) {
                vbo.draw();
            }
        }
    }
}
