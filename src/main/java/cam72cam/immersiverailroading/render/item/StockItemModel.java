package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.VBO;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class StockItemModel implements ItemRender.ISpriteItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		return new StandardModel().addCustom(() -> render(stack));
	}

	private void render(ItemStack stack) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);

		double scale = data.gauge.scale();
		if (data.def == null) {
			stack.setCount(0);
			return;
		}
		OBJRender model = StockRenderCache.getRender(data.def.defID);
		if (model == null) {
			stack.setCount(0);
			return;
		}

		try (
				OpenGL.With matrix = OpenGL.matrix();
				OpenGL.With tex = model.bindTexture(data.texture, false);
				OpenGL.With cull = OpenGL.bool(GL11.GL_CULL_FACE, false);
				VBO.BoundVBO vbo = StockRenderCache.getVBO(data.def.defID).bind();
		) {
				GL11.glTranslated(0.5, 0, 0);
				GL11.glRotated(-90, 0, 1, 0);
				scale = 0.2 * Math.sqrt(scale);
				GL11.glScaled(scale, scale, scale);
				vbo.draw();
		}
	}

	@Override
	public String getSpriteKey(ItemStack stack) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
		if (data.def == null) {
			// Stock pack removed
			System.out.println(stack.getTagCompound());
			return null;
		}
		return data.def.defID + data.def.getModel().hash;
	}

	@Override
	public StandardModel getSpriteModel(ItemStack stack) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
		EntityRollingStockDefinition def = data.def;
		return new StandardModel().addCustom(() -> {
			OBJRender model = StockRenderCache.getRender(def.defID);

			try (
					OpenGL.With matrix = OpenGL.matrix();
					OpenGL.With tex = model.bindTexture(null, true, true);
					VBO.BoundVBO vbo = StockRenderCache.getVBO(data.def.defID).bind();
			) {
				Gauge std = Gauge.from(Gauge.STANDARD);
				double modelLength = def.getLength(std);
				double size = Math.max(def.getHeight(std), def.getWidth(std));
				double scale = -1.6 / size;
				GL11.glTranslated(0, 0.85, -0.5);
				GL11.glScaled(scale, scale, scale / (modelLength / 2));
				GL11.glRotated(85, 0, 1, 0);
				vbo.draw();
			}
			model.textures.forEach((k, ts) -> ts.dealloc());
			model.icons.forEach((k, ts) -> ts.dealloc());
		});
	}
}
