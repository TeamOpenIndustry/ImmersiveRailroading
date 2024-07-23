package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class MultiblockModel extends OBJModel {
    private static final OBJModel item_input;
    private static final OBJModel item_output;
    private static final OBJModel fluid_input;
    private final MultiblockDefinition definition;

    private static final int ITEM_GRAVITY = 16;//m*s^-1

    public ModelState state;

    public StandardModel statics = new StandardModel();
    public ModelComponent itemOutputPoint;
    public List<ModelComponent> fluidHandlerPoints;

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
        this.definition = def;
        this.fluidHandlerPoints = new LinkedList<>();

        ComponentProvider provider = new ComponentProvider(this, 1, Collections.emptyMap());
        this.itemOutputPoint = provider.parse(ModelComponentType.ITEM_OUTPUT);
        this.fluidHandlerPoints = provider.parseAll(ModelComponentType.FLUID_HANDLER);

        this.state = ModelState.construct(settings -> settings.add(
                (ModelState.GroupVisibility) (animatable, group) -> !(group.contains("FLUID_HANDLER")||group.contains("ITEM_OUTPUT"))));
        this.state.include(itemOutputPoint);
        this.state.include(fluidHandlerPoints);
        this.state.include(provider.parse(ModelComponentType.WIDGET_X));
        this.state.include(provider.parse(ModelComponentType.REMAINING));
    }

    public void generateStaticModels(){
        //Storage statics
        for (Vec3i input : definition.itemInputPoints) {
            statics.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1, 1).translate(-input.x, input.y, -input.z);
                try (VBO.Binding vbo = item_input.binder().bind(state1)) {
                    vbo.draw();
                }
            });
        }

        for (Vec3i input : definition.fluidHandlePoints) {
            statics.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1,1).translate(-input.x, input.y, -input.z);
                try (VBO.Binding vbo = fluid_input.binder().bind(state1)) {
                    vbo.draw();
                }
            });
        }

        Vec3d offset = definition.itemOutputPoint;
        Vec3d velocity = definition.initialVelocity;
        if(offset == null){
            return;
        }
        if(velocity == null){
            velocity = Vec3d.ZERO;
        }
        velocity.add(0,10,0);
        float t = 0;
        while (t <= 2f && t * (velocity.y - ITEM_GRAVITY * t * 0.5) >= -5){
            velocity = new Vec3d(velocity.x * 0.99, velocity.y, velocity.z * 0.99);
            final Vec3d finalVelocity = velocity;
            float finalT = t;
            statics.addCustom((s, t1) -> {
                RenderState state1 = s.lighting(true).lightmap(1,1)
                        .translate(-offset.x, offset.y, -offset.z)
                        .translate(-finalT * (finalVelocity.x - 0.04),
                                finalT * (finalVelocity.y - ITEM_GRAVITY * finalT * 0.5),
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
