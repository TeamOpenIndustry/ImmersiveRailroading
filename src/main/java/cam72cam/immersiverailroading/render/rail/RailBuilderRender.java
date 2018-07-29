package cam72cam.immersiverailroading.render.rail;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class RailBuilderRender {
	
	private static OBJRender baseRailModel;
	private static OBJRender baseRailModelModel;
	
	static {
		try {
			baseRailModel = new OBJRender(new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m.obj"), 0.05f));
		} catch (Exception e) {
			ImmersiveRailroading.catching(e);
		}
		try {
			baseRailModelModel = new OBJRender(new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m_model.obj"), 0.05f));
		} catch (Exception e) {
			ImmersiveRailroading.catching(e);
		}
	}
	
	public static OBJRender getModel(Gauge gauge) {
		return gauge.isModel() ? baseRailModel : baseRailModelModel;
	}

	private static DisplayListCache displayLists = new DisplayListCache();
	public static void renderRailBuilder(RailInfo info) {
		
		OBJRender model = info.gauge.isModel() ? baseRailModel : baseRailModelModel;

		GL11.glTranslated(-info.position.getX(), -info.position.getY(), -info.position.getZ());
		GL11.glTranslated(info.placementPosition.x, info.placementPosition.y, info.placementPosition.z); 

		String renderID = RailRenderUtil.renderID(info);
		Integer displayList = displayLists.get(renderID);
		if (displayList == null) {

			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}
			
			displayList = ClientProxy.renderCacheLimiter.newList(() -> {		
			
			for (VecYawPitch piece : info.getBuilder().getRenderData()) {
				GL11.glPushMatrix();
				GL11.glRotatef(180-info.facing.getOpposite().getHorizontalAngle(), 0, 1, 0);
				GL11.glTranslated(piece.x, piece.y, piece.z);
				GL11.glRotatef(piece.getYaw(), 0, 1, 0);
				GL11.glRotatef(piece.getPitch(), 1, 0, 0);
				GL11.glRotatef(-90, 0, 1, 0);
				
				if (piece.getGroups().size() != 0) {
					if (piece.getLength() != -1) {
						GL11.glScaled(piece.getLength() / info.gauge.scale(), 1, 1);
					}
					
					// TODO static
					ArrayList<String> groups = new ArrayList<String>();
					for (String baseGroup : piece.getGroups()) {
						for (String groupName : model.model.groups())  {
							if (groupName.contains(baseGroup)) {
								groups.add(groupName);
							}
						}
					}

					
					model.drawDirectGroups(groups, info.gauge.scale());
				} else {
					model.drawDirect(info.gauge.scale());
				}
				GL11.glPopMatrix();
			}

			});
			displayLists.put(renderID, displayList);
		}
		
		model.bindTexture();
		Minecraft.getMinecraft().mcProfiler.startSection("dl");
		GL11.glCallList(displayList);
		Minecraft.getMinecraft().mcProfiler.endSection();;
		model.restoreTexture();
	}
}
