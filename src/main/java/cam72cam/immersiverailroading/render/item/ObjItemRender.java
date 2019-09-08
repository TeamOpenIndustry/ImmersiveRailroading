package cam72cam.immersiverailroading.render.item;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.function.BiFunction;

public class ObjItemRender {
    public static BiFunction<ItemStack, World, StandardModel> getModelFor(Identifier id, Vec3d translate, float scale) {
        try {
            OBJRender renderer = new OBJRender(new OBJModel(id, 0));
            return (stack, world) -> new StandardModel().addCustom(() -> {
                GL11.glPushMatrix();
                {
                    renderer.bindTexture();
                    GL11.glTranslated(translate.x, translate.y, translate.z);
                    GL11.glScaled(scale, scale, scale);
                    renderer.draw();
                    renderer.restoreTexture();
                }
                GL11.glPopMatrix();
            });
        } catch (Exception e) {
            throw new RuntimeException("Error loading item model...", e);
        }
    }
}
