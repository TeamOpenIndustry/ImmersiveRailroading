package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.util.ResourceLocation;

public class SteamHammerRender implements IMultiblockRender {
	private OBJRender renderer;
	private ArrayList<String> hammer;
	private ArrayList<String> rest;

	public SteamHammerRender() {
		try {
			this.renderer = new OBJRender(new OBJModel(new ResourceLocation("immersiverailroading:models/multiblocks/steam_hammer.obj"), -0.1f));
			this.hammer = new ArrayList<String>();
			this.rest = new ArrayList<String>();
			for (String group : renderer.model.groups()) {
				if (group.contains("Hammer")) {
					hammer.add(group);
				} else {
					rest.add(group);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void render(TileMultiblock te, double x, double y, double z, float partialTicks) {
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		//GL11.glScaled(2, 2, 2);
		GL11.glRotated(te.getRotation(), 0, 1, 0);
		renderer.drawGroups(rest);
		GL11.glTranslated(0, -(Math.abs(te.getRenderTicks() % 30 - 15)) / 14f, 0);
		renderer.drawGroups(hammer);
		GL11.glPopMatrix();
		
		tex.restore();
	}
}
