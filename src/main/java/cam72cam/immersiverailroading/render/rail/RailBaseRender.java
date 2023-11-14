package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.List;

public class RailBaseRender {
	private static StandardModel getModel(RailInfo info, List<TrackBase> tracks) {
		StandardModel model = new StandardModel();
		if (!info.settings.railBed.isEmpty()) {
			for (TrackBase base : tracks) {
				Vec3i basePos = base.getPos();
				model.addItemBlock(info.settings.railBed, new Matrix4()
						.translate(basePos.x, basePos.y, basePos.z)
						.scale(1, base.getBedHeight() + 0.1f * (float) info.settings.gauge.scale(), 1)
				);
			}
		}
		return model;
	}

	private static final ExpireableMap<String, StandardModel> models = new ExpireableMap<>();
	public static void draw(RailInfo info, List<TrackBase> tracks, RenderState state) {
		StandardModel model = models.get(info.uniqueID);
		if (model == null) {
			model = getModel(info, tracks);
			models.put(info.uniqueID, model);
		}
		model.render(state);
	}
}
