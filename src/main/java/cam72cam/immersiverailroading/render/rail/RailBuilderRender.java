package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.VBA;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import util.Matrix4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static Map<String, VBA> vbaMap = new HashMap<String, VBA>();
	public static void renderRailBuilder(RailInfo info) {
		
		OBJRender model = info.settings.gauge.isModel() ? baseRailModel : baseRailModelModel;

		VBA vba = vbaMap.get(info.uniqueID);
		if (vba == null) {

			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}		

            List<VBA> vbas = new ArrayList<VBA>();
			
			for (VecYawPitch piece : info.getBuilder().getRenderData()) {
				Matrix4 m = new Matrix4();
				m.rotate(Math.toRadians(180-info.placementInfo.facing.getOpposite().getHorizontalAngle()), 0, 1, 0);
				m.translate(piece.x, piece.y, piece.z);
				m.rotate(Math.toRadians(piece.getYaw()), 0, 1, 0);
				m.rotate(Math.toRadians(piece.getPitch()), 1, 0, 0);
				m.rotate(Math.toRadians(-90), 0, 1, 0);

				if (piece.getLength() != -1) {
					m.scale(piece.getLength() / info.settings.gauge.scale(), 1, 1);
				}
				double scale = info.settings.gauge.scale();
				m.scale(scale, scale, scale);

				if (piece.getGroups().size() != 0) {
					// TODO static
					ArrayList<String> groups = new ArrayList<String>();
					for (String baseGroup : piece.getGroups()) {
						for (String groupName : model.model.groups())  {
							if (groupName.contains(baseGroup)) {
								groups.add(groupName);
							}
						}
					}

					
					vbas.add(model.createVBA(groups, 1, m));
				} else {
					vbas.add(model.createVBA(model.model.groups.keySet(), 1, m));
				}
			}

			vba = new VBA(vbas);
			vbaMap.put(info.uniqueID, vba);
		}
		
		model.bindTexture();
		Minecraft.getMinecraft().mcProfiler.startSection("dl");
		vba.draw();
		Minecraft.getMinecraft().mcProfiler.endSection();;
		model.restoreTexture();
	}
}
