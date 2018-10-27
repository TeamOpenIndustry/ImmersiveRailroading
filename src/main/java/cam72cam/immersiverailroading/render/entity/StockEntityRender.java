package cam72cam.immersiverailroading.render.entity;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.client.Minecraft;
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
		
		stock.renderTick(partialTicks);
		
		String def = stock.getDefinitionID();
		
		StockModel model = StockRenderCache.getRender(def);

		GL11.glPushMatrix();
		{
			GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, true);
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			//GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
	
			// Move to specified position
			GL11.glTranslated(x, y, z);
			GL11.glPushMatrix();
			{
				GL11.glRotatef(180 - entityYaw, 0, 1, 0);
				GL11.glRotatef(stock.rotationPitch, 1, 0, 0);
				GL11.glRotatef(-90, 0, 1, 0);
				model.draw(stock, partialTicks);
			}
			GL11.glPopMatrix();
			
			if (stock instanceof EntityRidableRollingStock) {
				RenderStaticRiders.render((EntityRidableRollingStock) stock, partialTicks);
			}
			
			//tex.restore();
			cull.restore();
			light.restore();
		}
		GL11.glPopMatrix();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
		return null;
	}
}
