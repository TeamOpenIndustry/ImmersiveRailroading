package cam72cam.immersiverailroading.render;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import util.Matrix4;

public class StockEntityRender extends Render<EntityRollingStock> {

	public StockEntityRender(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	public boolean shouldRender(EntityRollingStock livingEntity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks) {
		EntityRollingStockDefinition def = stock.getDefinition();

		StockModel model = def.getModel();
		Matrix4 defaultTransform = def.getDefaultTransformation();

		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);

		// Move to specified position
		GlStateManager.translate(x, y + 0.35, z);

		GlStateManager.rotate(180 - entityYaw, 0, 1, 0);
		GlStateManager.rotate(stock.rotationPitch, 1, 0, 0);
		FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
		Matrix4 transform = defaultTransform.copy().rotate(Math.toRadians(180), 0, 1, 0);
		matrix.put((float) transform.m00);
		matrix.put((float) transform.m01);
		matrix.put((float) transform.m02);
		matrix.put((float) transform.m03);
		matrix.put((float) transform.m10);
		matrix.put((float) transform.m11);
		matrix.put((float) transform.m12);
		matrix.put((float) transform.m13);
		matrix.put((float) transform.m20);
		matrix.put((float) transform.m21);
		matrix.put((float) transform.m22);
		matrix.put((float) transform.m23);
		matrix.put((float) transform.m30);
		matrix.put((float) transform.m31);
		matrix.put((float) transform.m32);
		matrix.put((float) transform.m33);
		matrix.flip();

		GlStateManager.multMatrix(matrix);
		
		
		model.draw(stock);
		
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
		return null;
	}
}
