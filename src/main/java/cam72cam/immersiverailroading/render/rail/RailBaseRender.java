package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;

public class RailBaseRender {
	private static StandardModel getModel(RailInfo info, World world) {
		StandardModel model = new StandardModel();
		if (!info.settings.railBed.isEmpty()) {
			for (TrackBase base : info.getBuilder(world).getTracksForRender()) {
				model.addItemBlock(info.settings.railBed, new Vec3d(base.getPos()), new Vec3d(1, base.getBedHeight() + 0.1f * (float) info.settings.gauge.scale(), 1));
			}
		}
		return model;
	}

	private static final ExpireableList<String, StandardModel> models = new ExpireableList<>();
	public static void draw(RailInfo info, World world, RenderState state) {
		StandardModel model = models.get(info.uniqueID);
		if (model == null) {
			model = getModel(info, world);
			models.put(info.uniqueID, model);
		}
		model.render(state);
	}
}
