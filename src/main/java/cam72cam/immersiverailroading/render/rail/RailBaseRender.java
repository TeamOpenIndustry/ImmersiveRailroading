package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.cutter.Plane;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.List;

public class RailBaseRender {
	private static StandardModel getModel(RailInfo info, List<TrackBase> tracks) {
		StandardModel model = new StandardModel();
		if (!info.settings.railBed.isEmpty()) {
			for (TrackBase base : tracks) {
				Vec3i basePos = base.getPos();
				float bedHeight = base.getBedHeight();
				Plane bedFace = base.getBedFace();

				if (bedFace != null) bedHeight = 1;
				Matrix4 matrix4;
				if(bedHeight > 0) matrix4 = new Matrix4().scale(1, bedHeight, 1);
				else if(bedHeight == 0) matrix4 = new Matrix4().scale(1, 1e-3, 1);
				else matrix4 = new Matrix4().translate(0, -bedHeight, 0).scale(1, 1 + bedHeight, 1);

				model.addItemBlock(info.settings.railBed, new Matrix4()
						.translate(basePos.x, basePos.y, basePos.z)
						.multiply(matrix4),
                        bedFace != null ? bedFace.offset(new Vec3d(basePos)) : null
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
