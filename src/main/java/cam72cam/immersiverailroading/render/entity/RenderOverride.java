package cam72cam.immersiverailroading.render.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.Vec3d;

public class RenderOverride {
	public static void renderStockAndParticles(float partialTicks) {
		Minecraft.getMinecraft().mcProfiler.startSection("ir_entity");

        GLBoolTracker color = new GLBoolTracker(GL11.GL_COLOR_MATERIAL, true);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();

		GlStateManager.enableAlpha();
		
        ICamera camera = new Frustum();
        Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        camera.setPosition(d0, d1, d2);
        
        List<EntityRollingStock> entities = Minecraft.getMinecraft().player.getEntityWorld().getEntities(EntityRollingStock.class, EntitySelectors.IS_ALIVE);
        for (EntityRollingStock entity : entities) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().mcProfiler.startSection("render_stock");
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        		Minecraft.getMinecraft().mcProfiler.endSection();;
        	}
        }
        
        GlStateManager.depthMask(false);
        
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        Vec3d ep = player.getPositionEyes(partialTicks);
        
        List<EntitySmokeParticle> smokeEnts = player.getEntityWorld().getEntities(EntitySmokeParticle.class, EntitySelectors.IS_ALIVE);
        Comparator<EntitySmokeParticle> compare = (EntitySmokeParticle e1, EntitySmokeParticle e2) -> {
        	Double p1 = e1.getPositionVector().distanceTo(ep);
        	Double p2 = e1.getPositionVector().distanceTo(ep);
        	return p1.compareTo(p2);
        };
        Collections.sort(smokeEnts,  compare);
        

		Minecraft.getMinecraft().mcProfiler.startSection("ir_particles");
		
        ParticleRender.shader.bind();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        for (EntitySmokeParticle entity : smokeEnts) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().mcProfiler.startSection("render_particle");
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        		Minecraft.getMinecraft().mcProfiler.endSection();;
        	}
        }

		blend.restore();
		tex.restore();
		cull.restore();
		light.restore();
		
		ParticleRender.shader.unbind();
		
		Minecraft.getMinecraft().mcProfiler.endSection();
        
        Minecraft.getMinecraft().entityRenderer.disableLightmap();;
        RenderHelper.disableStandardItemLighting();
        color.restore();
        
        GlStateManager.depthMask(true);
        
        Minecraft.getMinecraft().mcProfiler.endSection();;
	}
}
