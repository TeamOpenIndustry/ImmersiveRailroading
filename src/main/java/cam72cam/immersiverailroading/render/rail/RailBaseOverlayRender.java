package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public class RailBaseOverlayRender {
	private static DisplayListCache cache = new DisplayListCache() {
		@Override
		public int lifespan() {
			return 1;
		}
		@Override
		public boolean sliding() {
			return false;
		}
	};

	private static void doDraw(RailInfo info) {
		GL11.glColor4f(1, 0, 0, 1);

		BlockPos placePos = new BlockPos(info.placementInfo.placementPosition);

		for (TrackBase base : info.getBuilder(placePos).getTracksForRender()) {
			boolean canPlace = base.canPlaceTrack();
			if (! canPlace) {
				GL11.glPushMatrix();
				BlockPos pos = base.getPos();
				pos = pos.subtract(placePos);
				GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ()+1);
				GL11.glScaled(1, base.getBedHeight() + 0.2f, 1);

				GL11.glBegin(GL11.GL_QUADS);
				// front
			    GL11.glVertex3f(0.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 1.0f, 0.0f);
			    GL11.glVertex3f(0.0f, 1.0f, 0.0f);
			    // back
			    GL11.glVertex3f(0.0f, 0.0f, -1.0f);
			    GL11.glVertex3f(1.0f, 0.0f, -1.0f);
			    GL11.glVertex3f(1.0f, 1.0f, -1.0f);
			    GL11.glVertex3f(0.0f, 1.0f, -1.0f);
			    // right
			    GL11.glVertex3f(1.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 0.0f, -1.0f);
			    GL11.glVertex3f(1.0f, 1.0f, -1.0f);
			    GL11.glVertex3f(1.0f, 1.0f, 0.0f);
			    // left
			    GL11.glVertex3f(0.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(0.0f, 0.0f, -1.0f);
			    GL11.glVertex3f(0.0f, 1.0f, -1.0f);
			    GL11.glVertex3f(0.0f, 1.0f, 0.0f);
			    // top
			    GL11.glVertex3f(0.0f, 1.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 1.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 1.0f, -1.0f);
			    GL11.glVertex3f(0.0f, 1.0f, -1.0f);
			    // bottom
			    GL11.glVertex3f(0.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 0.0f, 0.0f);
			    GL11.glVertex3f(1.0f, 0.0f, -1.0f);
			    GL11.glVertex3f(0.0f, 0.0f, -1.0f);
				GL11.glEnd();

			    GL11.glPopMatrix();
			}
		}
	}

	public static void draw(RailInfo info) {
		String key = info.uniqueID + new BlockPos(info.placementInfo.placementPosition);
		Integer displayList = cache.get(key);
		if (displayList == null) {
			displayList = GL11.glGenLists(1);
			{
				GL11.glNewList(displayList, GL11.GL_COMPILE);
				GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
				GLBoolTracker color = new GLBoolTracker(GL11.GL_COLOR_MATERIAL, true);
				GL11.glPushMatrix();
				doDraw(info);
				GL11.glPopMatrix();
				tex.restore();
				color.restore();
			}
			GL11.glEndList();
			cache.put(key, displayList);
		} else {
			GL11.glCallList(displayList);
		}

	}
}
