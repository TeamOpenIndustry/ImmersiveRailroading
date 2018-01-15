package cam72cam.immersiverailroading.render.entity;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.client.Minecraft;
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
		return false;
	}

	@Override
	public void doRender(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks) {
		Minecraft.getMinecraft().mcProfiler.startSection("ir_stock");
		
		String def = stock.getDefinitionID();
		
		StockModel model = StockRenderCache.getRender(def);

		GlStateManager.pushMatrix();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, true);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		//GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);

		// Move to specified position
		GlStateManager.translate(x, y + 0.35 * stock.gauge.scale(), z);

		GlStateManager.rotate(180 - entityYaw, 0, 1, 0);
		GlStateManager.rotate(stock.rotationPitch, 1, 0, 0);
		GlStateManager.rotate(-90, 0, 1, 0);
		model.draw(stock, partialTicks);
		
		//tex.restore();
		cull.restore();
		light.restore();

		GlStateManager.popMatrix();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
		return null;
	}
}
