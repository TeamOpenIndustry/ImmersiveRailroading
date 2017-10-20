package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.tile.TileSteamHammer;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class TileSteamHammerRender extends TileEntitySpecialRenderer<TileSteamHammer> {
	
	private OBJRender renderer;
	private List<String> hammer;
	private List<String> rest;

	public TileSteamHammerRender() {
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

	@Override
	public boolean isGlobalRenderer(TileSteamHammer te) {
		return true;
	}
	
	@Override
	public void render(TileSteamHammer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glScaled(2, 2, 2);
		renderer.drawGroups(rest);
		GL11.glTranslated(0, -(Math.abs(te.getTicks() % 30 - 15)) / 14f, 0);
		renderer.drawGroups(hammer);
		GL11.glPopMatrix();
		
		tex.restore();
	}
}
