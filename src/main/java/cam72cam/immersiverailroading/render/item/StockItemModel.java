package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.common.ModelConfig;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

public class StockItemModel implements ItemRender.ISpriteItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		return new StandardModel().addCustom((state, pt) -> render(stack, state));
	}

	private void render(ItemStack stack, RenderState state) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);

		double scale = data.gauge.scale();
		scale = 0.2 * Math.sqrt(scale);

		if (data.def == null) {
			stack.setCount(0);
			return;
		}

		state.cull_face(false)
				.translate(0.5, 0, 0)
				.rotate(-90, 0, 1, 0)
				.scale(scale, scale, scale);
		ModelConfig cfg = new ModelConfig().variant(data.texture);
		try (ModelRenderer.Binding vbo = ModelRenderer.getRendererFor(data.def.getModel().model).bind(cfg, state)) {
			vbo.enqueueOpaque(data.def.itemGroups);
		}
	}

	@Override
	public Identifier getSpriteKey(ItemStack stack) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
		if (data.def == null) {
			// Stock pack removed
			//System.out.println(stack.getTagCompound());
			return null;
		}
		return new Identifier(
				ImmersiveRailroading.MODID,
				data.def.defID + "_" +
						data.def.getModel().model.hash + "_" +
						(!ConfigGraphics.stockItemVariants || data.texture == null ? "" : data.texture)
		);
	}

	@Override
	public StandardModel getSpriteModel(ItemStack stack) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
		EntityRollingStockDefinition def = data.def;

		return new StandardModel().addCustom((state, pt) -> {
			Gauge std = Gauge.from(Gauge.STANDARD);
			double modelLength = def.getLength(std);
			double size = Math.max(def.getHeight(std), def.getWidth(std));
			double scale = -1.6 / size;
			state.translate(0, 0.85, -0.5);
			state.scale(scale, scale, scale / (modelLength / 2));
			state.rotate(85, 0, 1, 0);

			ModelConfig cfg = new ModelConfig().synchronous().lod(StockModel.LOD_SMALL);
			if (ConfigGraphics.stockItemVariants) {
				cfg.variant(data.texture);
			}

			Model model = def.getModel().model;
			try (ModelRenderer.Binding vbo =
						 ModelRenderer.getRendererFor(model).bind(cfg, state, true)) {
				vbo.enqueueOpaque(def.itemGroups);
			}
			model.free();
		});
	}
}
