package cam72cam.immersiverailroading.render.multiblock;

import cam72cam.mod.util.Identifier;
import org.lwjgl.opengl.GL11;

import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.GLBoolTracker;

public class PlateRollerRender implements IMultiblockRender {
	private OBJRender renderer;

	public PlateRollerRender() {
		try {
			this.renderer = new OBJRender(new OBJModel(new Identifier("immersiverailroading:models/multiblocks/plate_rolling_machine.obj"), 0.1f));
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
		GL11.glTranslated(-2.25, 0, 0.5);
		renderer.draw();
		GL11.glPopMatrix();
		
		this.renderer.restoreTexture();
		tex.restore();
	}
}
