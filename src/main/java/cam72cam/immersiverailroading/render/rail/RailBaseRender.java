package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;
import org.lwjgl.opengl.GL11;

public class RailBaseRender {
	private static synchronized void drawSync(RailInfo info) {
		if (info.settings.railBed.isEmpty()) {
			return;
		}

		StandardModel model = new StandardModel();

		for (TrackBase base : info.getBuilder().getTracksForRender()) {
			model.addItemBlock(info.settings.railBed, new Vec3d(base.getPos()), new Vec3d(1, base.getBedHeight() + 0.1f * (float)info.settings.gauge.scale(), 1));
		}

		model.render();
	}

	private static DisplayListCache displayLists = new DisplayListCache();
	public static void draw(RailInfo info) {
		Integer displayList = displayLists.get(info.uniqueID);
		if (displayList == null) {
			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}
			
			displayList = ClientProxy.renderCacheLimiter.newList(() ->drawSync(info));
			displayLists.put(info.uniqueID, displayList);
		}
		GL11.glCallList(displayList);
	}
}
