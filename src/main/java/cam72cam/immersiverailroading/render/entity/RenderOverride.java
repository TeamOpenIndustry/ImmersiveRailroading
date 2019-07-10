package cam72cam.immersiverailroading.render.entity;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.RailInstance;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockEntityInstance;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.math.Vec3d;
import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import java.util.*;

public class RenderOverride {

	public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
	}
	
	private static ICamera getCamera(float partialTicks) {
        ICamera camera = new Frustum();
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        return camera;
	}
	
	private static boolean isInRenderDistance(Vec3d pos) {
		// max rail length is 100, 50 is center
		return MinecraftClient.getPlayer().getPosition().distanceTo(pos) < ((Minecraft.getMinecraft().gameSettings.renderDistanceChunks+1) * 16 + 50);
	}
	
	private static final Predicate<Entity> IN_RENDER_DISTANCE = p_apply_1_ -> isInRenderDistance(p_apply_1_.getPosition());
	private static final Predicate<net.minecraft.entity.Entity> _IN_RENDER_DISTANCE = p_apply_1_ -> isInRenderDistance(new Vec3d(p_apply_1_.getPositionVector()));

	public static void renderStock(float partialTicks) {
        int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 0 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
        
		Minecraft.getMinecraft().mcProfiler.startSection("ir_entity");

        ICamera camera = getCamera(partialTicks);
        
        World world = MinecraftClient.getPlayer().getWorld();
        List<EntityRollingStock> entities = world.getEntities(EntityRollingStock.class);
        for (EntityRollingStock entity : entities) {
        	if (camera.isBoundingBoxInFrustum(entity.internal.getRenderBoundingBox())) {
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity.internal, partialTicks, true);
        	}
        }

        Minecraft.getMinecraft().mcProfiler.endSection();;
	}
	
	public static void renderParticles(float partialTicks) {
		int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 1 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
		Minecraft.getMinecraft().mcProfiler.startSection("ir_particles");
		
		GlStateManager.depthMask(false);
		
        ICamera camera = getCamera(partialTicks);
        Vec3d ep = getCameraPos(partialTicks);
        
        World world = MinecraftClient.getPlayer().getWorld();
        List<EntitySmokeParticle> smokeEnts = world.internal.getEntities(EntitySmokeParticle.class, _IN_RENDER_DISTANCE);
        Comparator<EntitySmokeParticle> compare = (EntitySmokeParticle e1, EntitySmokeParticle e2) -> {
        	Double p1 = e1.getPositionVector().squareDistanceTo(ep.internal);
        	Double p2 = e1.getPositionVector().squareDistanceTo(ep.internal);
        	return p1.compareTo(p2);
        };
        Minecraft.getMinecraft().mcProfiler.startSection("ent_sort");
    	Collections.sort(smokeEnts,  compare);
        Minecraft.getMinecraft().mcProfiler.endSection();

        ParticleRender.shader.bind();
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, false);
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		Minecraft.getMinecraft().mcProfiler.startSection("render_particle");
        for (EntitySmokeParticle entity : smokeEnts) {
        	if (camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox()) ) {
        		Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true);
        	}
        }
        Minecraft.getMinecraft().mcProfiler.endSection();

		blend.restore();
		tex.restore();
		cull.restore();
		light.restore();
		
		ParticleRender.shader.unbind();
		

		GlStateManager.depthMask(true);
		
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	public static void renderTiles(float partialTicks) {
        int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 0 && ConfigGraphics.useShaderFriendlyRender) {
        	return;
        }
        
		Minecraft.getMinecraft().mcProfiler.startSection("ir_tile");

        ICamera camera = getCamera(partialTicks);
        Vec3d cameraPos = getCameraPos(partialTicks);


		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, false);
	
        List<TileEntity> entities = new ArrayList<TileEntity>(Minecraft.getMinecraft().player.getEntityWorld().loadedTileEntityList);
        for (TileEntity tea : entities) {
        	if (!(tea instanceof BlockEntity.Internal)) {
        		continue;
			}
			BlockEntityInstance te = ((BlockEntity.Internal) tea).instance();
        	if (te instanceof RailInstance) {
        		if (!((RailInstance) te).isLoaded()) {
        			continue;
        		}
	        	if (true) { // TODO RENDER camera.isBoundingBoxInFrustum(te.getRenderBoundingBox()) && isInRenderDistance(new Vec3d(((RailInstance) te).pos))) {

	        		RailInfo info = ((RailInstance) te).info;
	        		if (info == null) {
	        			// Still loading...
	        			continue;
	        		}

	        		GL11.glPushMatrix();
	        		{
	        	        int i = te.world.internal.getCombinedLight(te.pos.internal, 0);
	        	        int j = i % 65536;
	        	        int k = i / 65536;
	        	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);
	        			if (info.settings.type == TrackItems.SWITCH) {
	        				//TODO render switch and don't render turn
	        				info = info.withType(TrackItems.STRAIGHT);
	        			}

						Vec3d pos = info.placementInfo.placementPosition.subtract(cameraPos);
						GL11.glTranslated(pos.x, pos.y, pos.z);

		        		RailBuilderRender.renderRailBuilder(info);
	        		}
	        		GL11.glPopMatrix();
	        	}	
        	}
        }
		blend.restore();
        /* TODO RENDER

        ClientProxy proxy = (ClientProxy) ImmersiveRailroading.proxy;
		Collection<TileRailPreview> previews = proxy.getPreviews();
		if (previews != null && previews.size() > 0) {
			Minecraft.getMinecraft().mcProfiler.startSection("tile_rail_preview");
			blend = new GLBoolTracker(GL11.GL_BLEND, true);
			GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
			if (GLContext.getCapabilities().OpenGL14) {
				GL14.glBlendColor(1, 1, 1, 0.7f);
			}
			for (TileRailPreview preview : previews) {
				if (!preview.hasWorld()) {
					preview.setWorld(Minecraft.getMinecraft().player.world);
				}
				for (BuilderBase builder : ((IIterableTrack) preview.getRailRenderInfo().getBuilder(preview.pos)).getSubBuilders()) {
					RailInfo info = builder.info;
					Vec3d placementPosition = info.placementInfo.placementPosition;

					if (isInRenderDistance(placementPosition)) {
						placementPosition = placementPosition.subtract(cameraPos);
                        GL11.glPushMatrix();
                        {
                            GL11.glTranslated(placementPosition.x, placementPosition.y, placementPosition.z);

                            RailRenderUtil.render(info, true);
                        }
                        GL11.glPopMatrix();
					}
				}
			}
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			if (GLContext.getCapabilities().OpenGL14) {
				GL14.glBlendColor(1, 1, 1, 1f);
			}
			blend.restore();
			Minecraft.getMinecraft().mcProfiler.endSection();
		}

        Minecraft.getMinecraft().mcProfiler.endSection();;
        */
	}
}
 