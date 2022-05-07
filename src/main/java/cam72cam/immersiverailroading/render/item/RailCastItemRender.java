package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class RailCastItemRender implements ItemRender.IItemModel {
	private static OBJModel model;
	private static List<String> groups;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if (model == null) {
			try {
				model = new OBJModel(new Identifier(ImmersiveRailroading.MODID, "models/multiblocks/rail_machine.obj"), 0.05f, null);
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
			try (OBJRender.Binding ctx = model.binder().bind(state)) {
				ctx.draw(groups);
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
