package cam72cam.immersiverailroading.render.entity;

import cam72cam.immersiverailroading.ConfigGraphics;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class MagicEntityRender extends Render<MagicEntity> {

	public MagicEntityRender(RenderManager renderManager) {
		super(renderManager);
	}
	
	@Override
	public boolean shouldRender(MagicEntity entity, ICamera camera, double camX, double camY, double camZ) {
		return ConfigGraphics.useShaderFriendlyRender;
	}

	@Override
	public void doRender(MagicEntity particle, double x, double y, double z, float entityYaw, float partialTicks) {
		RenderOverride.renderTiles(partialTicks);
		RenderOverride.renderStock(partialTicks);
		RenderOverride.renderParticles(partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(MagicEntity entity) {
		return null;
	}

}
