package cam72cam.immersiverailroading.render.rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class RailBuilderRender {
	
	private static OBJModel baseRailModel;
	
	static {
		try {
			baseRailModel = new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m.obj"));
		} catch (Exception e) {
			ImmersiveRailroading.logger.catching(e);
		}
	}

	private static Map<String, Integer> displayLists = new HashMap<String, Integer>();
	public static void renderRailBuilder(RailInfo info) {

		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GlStateManager.rotate(180-info.facing.getOpposite().getHorizontalAngle(), 0, 1, 0);
		GlStateManager.translate(info.horizOff-0.5, 0, 0);
		GlStateManager.rotate(-(180-info.facing.getOpposite().getHorizontalAngle()), 0, 1, 0);

		if (!displayLists.containsKey(RailRenderUtil.renderID(info))) {
			int displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			
			switch (info.facing) {
			case EAST:
				GlStateManager.translate(0, 0, 1);
				break;
			case NORTH:
				GlStateManager.translate(1, 0, 1);
				break;
			case SOUTH:
				// No Change
				break;
			case WEST:
				GlStateManager.translate(1, 0, 0);
				break;
			default:
				break;
			}			
			
			for (VecYawPitch piece : info.getBuilder().getRenderData()) {
				GlStateManager.pushMatrix();
				GlStateManager.rotate(180-info.facing.getOpposite().getHorizontalAngle(), 0, 1, 0);
				GlStateManager.translate(piece.x, piece.y, piece.z);
				GlStateManager.rotate(piece.getPitch(), 1, 0, 0);
				GlStateManager.rotate(piece.getYaw(), 0, 1, 0);
				GlStateManager.rotate(-90, 0, 1, 0);
				GlStateManager.scale(piece.getLength(), 1, 1);
				if (piece.getGroups().size() != 0) {
					// TODO static
					ArrayList<String> groups = new ArrayList<String>();
					for (String baseGroup : piece.getGroups()) {
						for (String groupName : baseRailModel.groups())  {
							if (groupName.contains(baseGroup)) {
								groups.add(groupName);
							}
						}
					}
					
					baseRailModel.drawDirectGroups(groups);
				} else {
					baseRailModel.drawDirect();
				}
				GlStateManager.popMatrix();
			}

			GL11.glEndList();
			
			displayLists.put(RailRenderUtil.renderID(info), displayList);
		}
		
		GL11.glCallList(displayLists.get(RailRenderUtil.renderID(info)));
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
}
