package cam72cam.immersiverailroading.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.obj.OBJModel;
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

		OBJModel model = def.getModel();
		Matrix4 defaultTransform = def.getDefaultTransformation();

		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);

		// Move to specified position
		GlStateManager.translate(x, y + 0.3, z);

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
		
		if (stock instanceof EntityMoveableRollingStock && ((EntityMoveableRollingStock)stock).frontYaw != null && ((EntityMoveableRollingStock)stock).rearYaw != null) {
			List<String> main = new ArrayList<String>();
			List<String> front = new ArrayList<String>();
			List<String> rear = new ArrayList<String>();
			
			for (String group : model.groups()) {
				if (group.contains("BOGEY_FRONT")) {
					front.add(group);
				} else if (group.contains("BOGEY_REAR")) {
					rear.add(group);
				} else {
					main.add(group);
				}
			}

			model.drawGroups(main);
			
			GlStateManager.pushMatrix();
			GlStateManager.translate(-def.getBogeyFront(), 0, 0);
			if (!((EntityMoveableRollingStock)stock).isReverse) {
				GlStateManager.rotate(180-((EntityMoveableRollingStock)stock).frontYaw, 0, 1, 0);							
			} else {
				GlStateManager.rotate(180-((EntityMoveableRollingStock)stock).rearYaw, 0, 1, 0);
			}
			GlStateManager.rotate(-(180-stock.rotationYaw), 0, 1, 0);
			GlStateManager.translate(def.getBogeyFront(), 0, 0);
			model.drawGroups(front);
			GlStateManager.popMatrix();
			
			GlStateManager.pushMatrix();
			GlStateManager.translate(-def.getBogeyRear(), 0, 0);
			if (!((EntityMoveableRollingStock)stock).isReverse) {
				GlStateManager.rotate(180-((EntityMoveableRollingStock)stock).rearYaw, 0, 1, 0);							
			} else {
				GlStateManager.rotate(180-((EntityMoveableRollingStock)stock).frontYaw, 0, 1, 0);
			}
			GlStateManager.rotate(-(180-stock.rotationYaw), 0, 1, 0);
			GlStateManager.translate(def.getBogeyRear(), 0, 0);
			model.drawGroups(rear);
			GlStateManager.popMatrix();
		} else {
			model.draw();
		}
		
		
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
		return null;
	}
}
