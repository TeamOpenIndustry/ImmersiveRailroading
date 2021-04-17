package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class StockItemComponentModel implements ItemRender.IItemModel {
    @Override
    public StandardModel getModel(World world, ItemStack stack) {
        return new StandardModel().addCustom(() -> StockItemComponentModel.render(stack));
    }
    public static void render(ItemStack stack) {
        ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(stack);
        double itemScale = data.gauge.scale();

        if (data.def == null) {
            ImmersiveRailroading.error("Item %s missing definition!", stack);
            stack.setCount(0);
            return;
        }

        OBJRender renderer = StockRenderCache.getRender(data.def.defID);
        ArrayList<String> groups = new ArrayList<>();

        for (ModelComponentType r : data.componentType.render) {
            ModelComponent comp = data.def.getComponent(r);
            if (comp == null || r == ModelComponentType.CARGO_FILL_X) {
                continue;
            }
            groups.addAll(comp.modelIDs);
        }

        Vec3d center = renderer.model.centerOfGroups(groups);
        double width = renderer.model.heightOfGroups(groups);
        double length = renderer.model.lengthOfGroups(groups);
        double scale = 1;
        if (width != 0 || length != 0) {
            scale = 0.95 / Math.max(width, length);
        }
        scale *= Math.sqrt(itemScale);

        try (
            OpenGL.With matrix = OpenGL.matrix();
            OpenGL.With tex = renderer.bindTexture(true);
            OpenGL.With blend = OpenGL.bool(GL11.GL_BLEND, false);
            OpenGL.With cull = OpenGL.bool(GL11.GL_CULL_FACE, false);
            OpenGL.With light = OpenGL.bool(GL11.GL_LIGHTING, false)
        ) {
            GL11.glTranslated(0.5, 0.5, 0.5);
            GL11.glScaled(scale, scale, scale);
            GL11.glTranslated(-center.x, -center.y, -center.z);
            renderer.drawGroups(groups);
        }
    }
}
