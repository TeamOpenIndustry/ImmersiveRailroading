package cam72cam.immersiverailroading.render;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

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

		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);

		// Move to specified position
		GlStateManager.translate(x, y + 0.35, z);

		GlStateManager.rotate(180 - entityYaw, 0, 1, 0);
		GlStateManager.rotate(stock.rotationPitch, 1, 0, 0);
		GlStateManager.rotate(-90, 0, 1, 0);
		
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
