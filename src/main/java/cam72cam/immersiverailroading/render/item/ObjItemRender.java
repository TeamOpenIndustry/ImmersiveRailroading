package cam72cam.immersiverailroading.render.item;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ObjItemRender {
    public static Map<Identifier, OBJModel> cache = new HashMap<>();

    public static ItemRender.IItemModel getModelFor(Identifier id, Vec3d translate, float scale) {
            return getModelFor(id, translate, Vec3d.ZERO, scale);
    }

    public static ItemRender.IItemModel getModelFor(Identifier id, Vec3d translate, Vec3d rotation, float scale) {
        return (stack, world) -> new StandardModel().addCustom((state, pt) -> {
            if (!cache.containsKey(id)) {
                try {
                    cache.put(id, new OBJModel(id, 0, null));
                } catch (Exception e) {
                    throw new RuntimeException("Error loading item model...", e);
                }
            }
            OBJModel model = cache.get(id);
            state.translate(translate.x, translate.y, translate.z)
                    .rotate(rotation.x, 1, 0, 0)
                    .rotate(rotation.y, 0, 1, 0)
                    .rotate(rotation.z, 0, 0, 1)
                    .scale(scale, scale, scale);
            try (OBJRender.Binding vbo = model.binder().bind(state)) {
                vbo.draw();
            }
        });
    }
}
