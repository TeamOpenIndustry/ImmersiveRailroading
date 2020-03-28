package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.entity.StockModel;
import cam72cam.mod.render.GLBoolTracker;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class StockItemModel implements ItemRender.ISpriteItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		return new StandardModel().addCustom(() -> render(stack));
	}

	private void render(ItemStack stack) {
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

	@Override
	public String getSpriteKey(ItemStack stack) {
		String defID = ItemDefinition.getID(stack);
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		if (def == null) {
			// Stock pack removed
			return null;
		}
		return defID + (def.getModel().hash + StockRenderCache.getRender(defID).textures.get(null).hash);
	}

	@Override
	public StandardModel getSpriteModel(ItemStack stack) {
		String defID = ItemDefinition.getID(stack);
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		// We want to upload the model even if the sprite is cached
		StockModel model = StockRenderCache.getRender(defID);

		return new StandardModel().addCustom(() -> {
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
            model.bindTexture(true);
            GL11.glPushMatrix();
			Gauge std = Gauge.from(Gauge.STANDARD);
            double modelLength = def.getLength(std);
            double size = Math.max(def.getHeight(std), def.getWidth(std));
            double scale = -1.6/size;
            GL11.glTranslated(0, 0.85, -0.5);
            GL11.glScaled(scale, scale, scale / (modelLength /2));
            GL11.glRotated(85, 0, 1, 0);
            model.draw();
            GL11.glPopMatrix();
            model.restoreTexture();
            model.textures.forEach((k, ts) -> ts.dealloc());
			tex.restore();
        });
	}
}
