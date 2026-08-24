package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class RailCastItemRender implements ItemRender.IItemModel {
	private static Model model;
	private static List<String> groups;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if (model == null) {
			try {
				model = ModelLoader.load(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/rail_machine.obj"));
				groups = new ArrayList<>();

				for (String groupName : model.groups())  {
					if (groupName.contains("INPUT_CAST")) {
						groups.add(groupName);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}


		return new StandardModel().addCustom((state, pt) -> {
			state.rotate(90, 1, 0, 0);
			state.translate(0, -1, 1);
			state.translate(-0.5, 0.6, 0.6);
			try (ModelRenderer.Binding bound = ModelRenderer.getRendererFor(model).bind(state)) {
				bound.enqueueOpaque(groups);
			}
		});
	}

	@Override
	public void applyTransform(ItemStack stack, ItemRender.ItemRenderType type, RenderState state) {
		ItemRender.IItemModel.defaultTransform(type, state);

		if (type == ItemRender.ItemRenderType.GUI) {
			state.scale(1, 0.1, 1);
		}
	}
}
