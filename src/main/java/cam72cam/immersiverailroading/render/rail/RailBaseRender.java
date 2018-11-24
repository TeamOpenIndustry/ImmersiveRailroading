package cam72cam.immersiverailroading.render.rail;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.proxy.ClientProxy;
import cam72cam.immersiverailroading.render.BakedModelCache;
import cam72cam.immersiverailroading.render.BakedScaledModel;
import cam72cam.immersiverailroading.render.DisplayListCache;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;

public class RailBaseRender {
	private static BlockRendererDispatcher blockRenderer;
	private static BakedModelCache scaled = new BakedModelCache();
	
	/*
	 * This returns a cached buffer as rails don't change their model often
	 * This drastically reduces the overhead of rendering these complex models
	 * 
	 * We also draw the railbed here since drawing a model for each gag eats FPS 
	 */
	private static BufferBuilder getBaseBuffer(RailInfo info) {
		if (blockRenderer == null) {
			// Get model for current state
			blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		}
		
		if (info.settings.railBed.getItem() == Items.AIR) {
			return null;
		}
		IBlockState gravelState = BlockUtil.itemToBlockState(info.settings.railBed);
		
		// Create render targets
		BufferBuilder worldRenderer = new BufferBuilder(2048);
		
		// Start drawing
		try {
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
	
			// From IE
			worldRenderer.color(255, 255, 255, 255);
			
			// This is evil but really fast :D
			for (TrackBase base : info.getBuilder().getTracksForRender()) {
				String key = gravelState.toString() + base.getBedHeight() + ":"  + info.settings.gauge.scale();
				IBakedModel model = scaled.get(key);
				if (model == null) {
					IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
					model = new BakedScaledModel(gravelModel, base.getBedHeight() + 0.1f * (float)info.settings.gauge.scale());
					scaled.put(key, model);
				}
				blockRenderer.getBlockModelRenderer().renderModel(info.world, model, gravelState, base.getPos(), worldRenderer, false);
			}
		} finally {
			worldRenderer.finishDrawing();
		}
		return worldRenderer;
	}
	
	private static synchronized void drawSync(RailInfo info) {
		RailRenderUtil.draw(getBaseBuffer(info));
	}

	private static DisplayListCache displayLists = new DisplayListCache();
	public static void draw(RailInfo info) {
		Integer displayList = displayLists.get(info.uniqueID);
		if (displayList == null) {
			if (!ClientProxy.renderCacheLimiter.canRender()) {
				return;
			}
			
			displayList = ClientProxy.renderCacheLimiter.newList(() ->drawSync(info));
			displayLists.put(info.uniqueID, displayList);
		}
		GL11.glCallList(displayList);
	}
}
