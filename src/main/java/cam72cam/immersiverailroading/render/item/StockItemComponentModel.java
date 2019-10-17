package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.entity.StockModel;
import cam72cam.mod.render.GLBoolTracker;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class StockItemComponentModel implements ItemRender.IItemModel {
    @Override
    public StandardModel getModel(World world, ItemStack stack) {
        return new StandardModel().addCustom(() -> StockItemComponentModel.render(stack));
    }
    public static void render(ItemStack stack) {
        double itemScale = ItemGauge.get(stack).scale();
        String defID = ItemDefinition.getID(stack);
        ItemComponentType item = ItemComponent.getComponentType(stack);
        EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);

        if (def == null) {
            ImmersiveRailroading.error("Item %s missing definition!", stack);
            stack.setCount(0);
            return;
        }

        StockModel renderer = StockRenderCache.getRender(defID);
        ArrayList<String> groups = new ArrayList<>();

        for (RenderComponentType r : item.render) {
            RenderComponent comp = def.getComponent(r, Gauge.from(Gauge.STANDARD));
            if (comp == null || r == RenderComponentType.CARGO_FILL_X) {
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

        GL11.glPushMatrix();
        {
            GL11.glTranslated(0.5, 0.5, 0.5);
            GL11.glScaled(scale, scale, scale);
            GL11.glTranslated(-center.x, -center.y, -center.z);

            GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, false);
            GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
            GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
            GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
            renderer.bindTexture(null, true);
            renderer.drawGroups(groups);
            renderer.restoreTexture();

            blend.restore();
            cull.restore();
            tex.restore();
            light.restore();
        }
        GL11.glPopMatrix();

        return;
    }
}
