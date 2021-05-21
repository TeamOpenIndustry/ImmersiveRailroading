package cam72cam.immersiverailroading.render.item;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class ObjItemRender {
    public static Map<Identifier, OBJRender> cache = new HashMap<>();

    public static ItemRender.IItemModel getModelFor(Identifier id, Vec3d translate, float scale) {
            return getModelFor(id, translate, Vec3d.ZERO, scale);
    }

    public static ItemRender.IItemModel getModelFor(Identifier id, Vec3d translate, Vec3d rotation, float scale) {
        return (stack, world) -> new StandardModel().addCustom(() -> {
            if (!cache.containsKey(id)) {
                try {
                    cache.put(id, new OBJRender(new OBJModel(id, 0, null)));
                } catch (Exception e) {
                    throw new RuntimeException("Error loading item model...", e);
                }
            }
            OBJRender renderer = cache.get(id);
            try (OpenGL.With matrix = OpenGL.matrix(); OpenGL.With tex = renderer.bindTexture()) {
                GL11.glTranslated(translate.x, translate.y, translate.z);
                GL11.glRotated(rotation.x, 1, 0, 0);
                GL11.glRotated(rotation.y, 0, 1, 0);
                GL11.glRotated(rotation.z, 0, 0, 1);
                GL11.glScaled(scale, scale, scale);
                renderer.draw();
            }
        });
    }
}
