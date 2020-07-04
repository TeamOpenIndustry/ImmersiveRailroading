package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class RailItemRender implements ItemRender.IItemModel {
	private static OBJRender baseRailModel;
	private static List<String> left;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if (baseRailModel == null) {
			baseRailModel = StockRenderCache.getTrackRenderer(DefinitionManager.getTracks().stream().findFirst().get().getTrackForGauge(0));
			List<String> groups = new ArrayList<>();

			for (String groupName : baseRailModel.model.groups())  {
				if (groupName.contains("RAIL_LEFT")) {
					groups.add(groupName);
				}
			}
			left = groups;
		}


		return new StandardModel().addCustom(() -> {
			try (OpenGL.With matrix = OpenGL.matrix(); OpenGL.With tex = baseRailModel.bindTexture()) {
                GL11.glTranslated(0.5, 0.2, -0.3);
                baseRailModel.drawGroups(left);
            }
		});
	}
}
