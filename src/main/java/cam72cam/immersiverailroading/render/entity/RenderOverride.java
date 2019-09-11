package cam72cam.immersiverailroading.render.entity;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntitySmokeParticle;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.math.Vec3d;
import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

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

	public static void renderTiles(float partialTicks) {
        int pass = MinecraftForgeClient.getRenderPass();
        if (pass != 0) {
        	return;
        }
        
		Minecraft.getMinecraft().mcProfiler.startSection("ir_tile");

        ICamera camera = getCamera(partialTicks);
        Vec3d cameraPos = getCameraPos(partialTicks);


		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, false);
	
        List<net.minecraft.tileentity.TileEntity> entities = new ArrayList<net.minecraft.tileentity.TileEntity>(Minecraft.getMinecraft().player.getEntityWorld().loadedTileEntityList);
        for (net.minecraft.tileentity.TileEntity tea : entities) {
        	if (!(tea instanceof TileEntity)) {
        		continue;
			}
			BlockEntity te = ((TileEntity) tea).instance();
        	if (te instanceof Rail) {
        		if (!((Rail) te).isLoaded()) {
        			continue;
        		}
	        	if (false) { // TODO RENDER camera.isBoundingBoxInFrustum(te.getRenderBoundingBox()) && isInRenderDistance(new Vec3d(((Rail) te).pos))) {

	        		RailInfo info = ((Rail) te).info;
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
 