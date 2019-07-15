package cam72cam.immersiverailroading.render.multiblock;

import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.util.Identifier;
import org.lwjgl.opengl.GL11;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.immersiverailroading.multiblock.BoilerRollerMultiblock.BoilerRollerInstance;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.GLBoolTracker;

public class BoilerRollerRender implements IMultiblockRender {
	private OBJRender renderer;
	private List<String> segments;
	private List<String> product;
	private List<String> rest;

	public BoilerRollerRender() {
		try {
			this.renderer = new OBJRender(new OBJModel(new Identifier("immersiverailroading:models/multiblocks/boiler_rolling_machine.obj"), 0.1f));
			segments = new ArrayList<>();
			product = new ArrayList<>();
			rest = new ArrayList<>();
			for (String name : renderer.model.groups.keySet()) {
				if (name.contains("SEGMENT_")) {
					segments.add(name);
				} else if (name.contains("FINISHED_PREVIEW")) {
					product.add(name);
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
		
		BoilerRollerInstance tmb = (BoilerRollerInstance) te.getMultiblock();
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslated(0.5, 0, 0.5);
		GL11.glRotated(te.getRotation()-90, 0, 1, 0);
		GL11.glTranslated(-3.35, 0, -2.5);
		
		//TODO better animation
		if (tmb.hasOutput()) {
			renderer.drawGroups(product);
		} else if (tmb.hasInput()) {
			renderer.drawGroups(segments);
		}
		
		renderer.drawGroups(rest);
		GL11.glPopMatrix();
		
		this.renderer.restoreTexture();
		tex.restore();
	}
}
