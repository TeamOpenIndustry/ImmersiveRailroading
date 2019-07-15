package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class RailItemRender {
	private static OBJRender baseRailModel;
	private static List<String> left;

	static {
		try {
			baseRailModel = StockRenderCache.getTrackRenderer(DefinitionManager.getTracks().stream().findFirst().get().getTrackForGauge(0));
			List<String> groups = new ArrayList<>();
			
			for (String groupName : baseRailModel.model.groups())  {
				if (groupName.contains("RAIL_LEFT")) {
					groups.add(groupName);
				}
			}
			left = groups;
		} catch (Exception e) {
			ImmersiveRailroading.catching(e);
		}
	}

	public static StandardModel getModel(ItemStack stack, World world) {
		GL11.glPushMatrix();
		{
			GL11.glTranslated(0.5, 0.2, -0.3);
			baseRailModel.bindTexture();
			baseRailModel.drawGroups(left);
			baseRailModel.restoreTexture();
		}
		GL11.glPopMatrix();
		return null;
	}
}
