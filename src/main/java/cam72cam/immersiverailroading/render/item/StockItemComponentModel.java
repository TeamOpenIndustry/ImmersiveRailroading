package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class StockItemComponentModel implements ItemRender.IItemModel {
    @Override
    public StandardModel getModel(World world, ItemStack stack) {
        return new StandardModel().addCustom((state, pt) -> StockItemComponentModel.render(stack, state));
    }
    public static void render(ItemStack stack, RenderState state) {
        ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(stack);
        double itemScale = data.gauge.scale();

        if (data.def == null) {
            ImmersiveRailroading.error("Item %s missing definition!", stack);
            stack.setCount(0);
            return;
        }

        StockModel<?> model = data.def.getModel();
        ArrayList<String> groups = new ArrayList<>();

        List<ModelComponent> comps = data.def.getComponents(data.componentType.render);
        for (ModelComponent comp : comps) {
            if (comp.type == ModelComponentType.CARGO_FILL_X) {
                continue;
            }
            groups.addAll(comp.modelIDs);
        }

        if (groups.isEmpty()) {
            ImmersiveRailroading.error("Invalid item %s", stack.toTag());
            return;
        }

        Vec3d center = model.centerOfGroups(groups);
        double width = model.heightOfGroups(groups);
        double length = model.lengthOfGroups(groups);
        double scale = 1;
        if (width != 0 || length != 0) {
            scale = 0.95 / Math.max(width, length);
        }
        scale *= Math.sqrt(itemScale);


        state.blend(BlendMode.OPAQUE)
                .cull_face(false)
                .lighting(false)
                .translate(0.5, 0.5, 0.5)
                .scale(scale, scale, scale)
                .translate(-center.x, -center.y, -center.z);

        try (OBJRender.Binding vbo = model.binder().bind(state)) {
            vbo.draw(groups);
        }
    }
}
