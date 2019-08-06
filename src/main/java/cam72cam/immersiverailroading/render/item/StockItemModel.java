package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.entity.StockModel;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

public class StockItemModel {

	public static StandardModel getModel(ItemStack stack, World world) {
		return new StandardModel().addCustom(() -> StockItemModel.render(stack));
	}

	public static void render(ItemStack stack) {
		double scale = ItemGauge.get(stack).scale();
		String defID = ItemDefinition.getID(stack);
		StockModel model = StockRenderCache.getRender(defID);
		if (model == null) {
			stack.setCount(0);
			return;
		}
		String texture = ItemTextureVariant.get(stack);

        GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
        GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);

        GL11.glPushMatrix();
		{
			GL11.glTranslated(0.5, 0, 0);
			GL11.glRotated(-90, 0, 1, 0);
			scale = 0.2 * Math.sqrt(scale);
			GL11.glScaled(scale, scale, scale);
			model.bindTexture(texture, true);
			model.draw();
			model.restoreTexture();
		}
        GL11.glPopMatrix();

        tex.restore();
        cull.restore();
	}

	public static Pair<String, StandardModel> getIcon(ItemStack stack) {
		String defID = ItemDefinition.getID(stack);
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		StockModel model = StockRenderCache.getRender(defID);

		return Pair.of(defID, new StandardModel().addCustom(() -> {
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
            model.bindTexture();
            GL11.glPushMatrix();
            double modelLength = model.model.lengthOfGroups(model.model.groups());
            double scale = -0.60 / def.recommended_gauge.value();
            GL11.glTranslated(0, 0.85, -0.5);
            GL11.glScaled(scale, scale, scale / (modelLength /2));
            GL11.glRotated(85, 0, 1, 0);
            model.draw();
            GL11.glPopMatrix();
            model.restoreTexture();
			tex.restore();
        }));
	}
}
