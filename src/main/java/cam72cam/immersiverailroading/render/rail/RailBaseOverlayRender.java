package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.world.World;
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

	private static void doDraw(RailInfo info, World world, Vec3i pos) {
		Vec3i placePos = new Vec3i(info.placementInfo.placementPosition).add(pos);

		for (TrackBase base : info.getBuilder(world, placePos).getTracksForRender()) {
			boolean canPlace = base.canPlaceTrack();
			if (! canPlace) {
				GL11.glPushMatrix();
				{
					Vec3i tpos = base.getPos();
					tpos = tpos.subtract(placePos);
					GL11.glTranslated(tpos.x, tpos.y, tpos.z + 1);
					GL11.glScaled(1.002, base.getBedHeight() + 0.2f, 1.002);
					GL11.glTranslated(-0.001, 0, 0.001);

					GL11.glBegin(GL11.GL_QUADS);
					// front
					GL11.glVertex3f(0.0f, 0.0f, 0.0f);
					GL11.glVertex3f(1.0f, 0.0f, 0.0f);
					GL11.glVertex3f(1.0f, 1.0f, 0.0f);
					GL11.glVertex3f(0.0f, 1.0f, 0.0f);
					// back
					GL11.glVertex3f(0.0f, 1.0f, -1.0f);
					GL11.glVertex3f(1.0f, 1.0f, -1.0f);
					GL11.glVertex3f(1.0f, 0.0f, -1.0f);
					GL11.glVertex3f(0.0f, 0.0f, -1.0f);
					// right
					GL11.glVertex3f(1.0f, 0.0f, 0.0f);
					GL11.glVertex3f(1.0f, 0.0f, -1.0f);
					GL11.glVertex3f(1.0f, 1.0f, -1.0f);
					GL11.glVertex3f(1.0f, 1.0f, 0.0f);
					// left
					GL11.glVertex3f(0.0f, 1.0f, 0.0f);
					GL11.glVertex3f(0.0f, 1.0f, -1.0f);
					GL11.glVertex3f(0.0f, 0.0f, -1.0f);
					GL11.glVertex3f(0.0f, 0.0f, 0.0f);
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
				}
				GL11.glPopMatrix();
			}
		}
	}

	public static void draw(RailInfo info, World world, Vec3i pos, RenderState state) {
		String key = info.uniqueID + pos.add(new Vec3i(info.placementInfo.placementPosition));
		Integer displayList = cache.get(key);
		if (displayList == null) {
			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			doDraw(info, world, pos);
			GL11.glEndList();
			cache.put(key, displayList);
		}
		state.texture(Texture.NO_TEXTURE);
		state.color(1, 0, 0, 1);
		try (OpenGL.With ctx = LegacyRenderContext.INSTANCE.apply(state)) {
			GL11.glCallList(displayList);
		}
	}
}
