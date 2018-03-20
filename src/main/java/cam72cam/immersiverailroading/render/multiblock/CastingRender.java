package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.CastingMultiblock.CastingInstance;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.util.ResourceLocation;

public class CastingRender implements IMultiblockRender {
	private OBJRender renderer;
	private List<String> flowing_steel;
	private List<String> steel_level;
	private List<String> rest;

	public CastingRender() {
		try {
			this.renderer = new OBJRender(new OBJModel(new ResourceLocation("immersiverailroading:models/multiblocks/casting_machine.obj"), 0.1f));
			flowing_steel = new ArrayList<String>();
			steel_level = new ArrayList<String>();
			rest = new ArrayList<String>();
			for (String name : renderer.model.groups.keySet()) {
				if (name.contains("FLOWING_STEEL")) {
					flowing_steel.add(name);
				} else if (name.contains("STEEL_LEVEL")) {
					steel_level.add(name);
				} else {
					rest.add(name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void render(TileMultiblock te, double x, double y, double z, float partialTicks) {
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, this.renderer.hasTexture());
		this.renderer.bindTexture();
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslated(0.5, 0, 0.5);
		GL11.glRotated(te.getRotation()-90, 0, 1, 0);
		GL11.glTranslated(-2.5, -3, 6.5);
		CastingInstance tmb = (CastingInstance) te.getMultiblock();
		if (tmb.isPouring()) {
			renderer.drawGroups(flowing_steel);
		}
		double steelLevel = tmb.getSteelLevel() * 4.5;
		if (steelLevel != 0) {
			GL11.glPushMatrix();
			GL11.glTranslated(0, steelLevel, 0);
			renderer.drawGroups(steel_level);
			GL11.glPopMatrix();
		}
		renderer.drawGroups(rest);;
		GL11.glPopMatrix();
		
		this.renderer.restoreTexture();
		tex.restore();
	}
}
