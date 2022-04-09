package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.List;

public class RailItemRender implements ItemRender.IItemModel {
	private static TrackModel baseRailModel;
	private static List<String> left;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if (baseRailModel == null) {
			baseRailModel = DefinitionManager.getTracks().stream().findFirst().get().getTrackForGauge(0);
			List<String> groups = new ArrayList<>();

			for (String groupName : baseRailModel.groups())  {
				if (groupName.contains("RAIL_LEFT")) {
					groups.add(groupName);
				}
			}
			left = groups;
		}


		return new StandardModel().addCustom((state, pt) -> {
			state.translate(0.5, 0.2, -0.3);
			try (OBJRender.Binding vbo = baseRailModel.binder().bind(state)) {
                vbo.draw(left);
            }
		});
	}
}
