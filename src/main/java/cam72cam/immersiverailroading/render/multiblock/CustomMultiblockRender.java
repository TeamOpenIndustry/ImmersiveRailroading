package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.MultiblockModel;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CustomMultiblockRender implements IMultiblockRender {
    public static final Vec3d north = new Vec3d(1, 0, 1);
    public static final Vec3d west = new Vec3d(1, 0, 0);
    public static final Vec3d east = new Vec3d(0, 0, 1);
    private static final OBJModel fluid_output;

    private static final Map<String, MultiblockDefinition> refer;

    static {
        refer = new HashMap<>();
        try {
            fluid_output = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/frames/fluid_output.obj"), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addDef(MultiblockDefinition def) {
        refer.put(def.name, def);
    }

    @Override
    public void render(TileMultiblock te, RenderState state, float partialTicks) {
        MultiblockModel base = refer.get(te.getName()).model;

        int temp = (int) (te.getRotation() - 90);
        Vec3i t = refer.get(te.getName()).center.rotate(te.geFacing());
        state.translate(-t.x, t.y, -t.z);
        state.rotate(temp, 0, 1, 0);
        switch (temp) {
            case 0:
                state.translate(north);
                break;
            case 90:
                state.translate(east);
                break;
            case -90:
                state.translate(west);
        }

        if (base != null) {
            try (OBJRender.Binding vbo = base.binder().bind(state)) {
                vbo.draw();
            }
            if (MinecraftClient.isReady() && MinecraftClient.getPlayer().getHeldItem(Player.Hand.SECONDARY).is(IRItems.ITEM_MANUAL)) {
                base.frames.render(state);
                for (Vec3i track : CustomTransporterMultiblock.packages.get(te.getPos()).trackList) {
                    RenderState state1 = state.clone().lighting(true).lightmap(1, 1).translate(-track.x, -track.y, -track.z);
                    try (VBO.Binding vbo = fluid_output.binder().bind(state1)) {
                        vbo.draw();
                    }
                }
            }
        }
    }
}
