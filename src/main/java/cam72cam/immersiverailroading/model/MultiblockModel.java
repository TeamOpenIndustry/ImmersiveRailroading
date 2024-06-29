package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;

public class MultiblockModel extends OBJModel {
    public final HashMap<String, Float> controlGroups;
    private static final OBJModel item_input;
    private static final OBJModel item_output;
    private static final OBJModel fluid_input;

    public StandardModel frames = new StandardModel();

    static {
        try {
            item_input = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/frames/item_input.obj"), 0);
            item_output = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/frames/item_output.obj"), 0);
            fluid_input = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/frames/fluid_input.obj"), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MultiblockModel(Identifier modelLoc, float darken, MultiblockDefinition def) throws Exception {
        super(modelLoc, darken);
        this.controlGroups = new HashMap<>();

        //Storage statics
        for (Vec3i input : def.itemInputPoints) {
            frames.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1, 1).translate(-input.x, input.y, -input.z);
                try (VBO.Binding vbo = item_input.binder().bind(state1)) {
                    vbo.draw();
                }
            });
        }

        for (Vec3i input : def.fluidInputPoints) {
            frames.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1,1).translate(-input.x, input.y, -input.z);
                try (VBO.Binding vbo = fluid_input.binder().bind(state1)) {
                    vbo.draw();
                }
            });
        }

        Vec3d offset = def.itemOutputPoint;
        Vec3d velocity = def.initialVelocity;
        final int GRAVITY = 16;//m*s^-1
        if(offset == null){
            return;
        }
        if(velocity == null){
            velocity = Vec3d.ZERO;
        }
        velocity.add(0,10,0);
        float t = 0;
        while (t <= 2f && t * (velocity.y - GRAVITY * t * 0.5) >= -5){
            velocity = new Vec3d(velocity.x * 0.99, velocity.y, velocity.z * 0.99);
            final Vec3d finalVelocity = velocity;
            float finalT = t;
            frames.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1,1)
                        .translate(-offset.x, offset.y, -offset.z)
                        .translate(-finalT * (finalVelocity.x - 0.04),
                                finalT * (finalVelocity.y - GRAVITY * finalT * 0.5),
                                -finalT * finalVelocity.z)
                        .scale(0.5,0.5,0.5);
                try (VBO.Binding vbo = item_output.binder().bind(state1)) {
                    vbo.draw();
                }
            });
            t += 0.1f;
        }
    }
}
