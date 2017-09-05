package cam72cam.immersiverailroading.render.rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

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

		Vec3d renderOff = new Vec3d(-0.5, 0, -0.5);

		switch (info.facing) {
		case EAST:
			renderOff = renderOff.addVector(0, 0, 1);
			break;
		case NORTH:
			renderOff = renderOff.addVector(1, 0, 1);
			break;
		case SOUTH:
			// No Change
			break;
		case WEST:
			renderOff = renderOff.addVector(1, 0, 0);
			break;
		default:
			break;
		}	
		
		renderOff = VecUtil.rotateYaw(renderOff, (info.direction == TrackDirection.LEFT ? -1 : 1) * info.quarter/4f * 90 - 90);
		GlStateManager.translate(renderOff.x, renderOff.y, renderOff.z);
		//GlStateManager.translate(info.getOffset().x, 0, info.getOffset().z);
		GlStateManager.translate(-info.position.getX(), -info.position.getY(), -info.position.getZ());
		GlStateManager.translate(info.placementPosition.x, info.placementPosition.y, info.placementPosition.z);

		if (!displayLists.containsKey(RailRenderUtil.renderID(info))) {
			int displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);		
			
			for (VecYawPitch piece : info.getBuilder().getRenderData()) {
				GlStateManager.pushMatrix();
				GlStateManager.rotate(180-info.facing.getOpposite().getHorizontalAngle(), 0, 1, 0);
				GlStateManager.translate(piece.x, piece.y, piece.z);
				GlStateManager.rotate(piece.getYaw(), 0, 1, 0);
				GlStateManager.rotate(piece.getPitch(), 1, 0, 0);
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
