package cam72cam.immersiverailroading.render.multiblock;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.util.ResourceLocation;

public class BoilerRollerRender implements IMultiblockRender {
	private OBJRender renderer;

	public BoilerRollerRender() {
		try {
			this.renderer = new OBJRender(new OBJModel(new ResourceLocation("immersiverailroading:models/multiblocks/boiler_rolling_machine.obj"), 0.1f));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void render(TileMultiblock te, double x, double y, double z, float partialTicks) {
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslated(0.5, 0, 0.5);
		GL11.glRotated(te.getRotation()-90, 0, 1, 0);
		GL11.glTranslated(-3.35, 0, 0.5);
		renderer.draw();
		GL11.glPopMatrix();
		
		tex.restore();
	}
}
