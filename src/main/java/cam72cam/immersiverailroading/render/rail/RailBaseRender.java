package cam72cam.immersiverailroading.render.rail;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;

public class RailBaseRender {
	private static Map<String, BufferBuilder> buffers = new HashMap<String, BufferBuilder>();
	
	/*
	 * This returns a cached buffer as rails don't change their model often
	 * This drastically reduces the overhead of rendering these complex models
	 * 
	 * We also draw the railbed here since drawing a model for each gag eats FPS 
	 */
	protected static BufferBuilder getBaseBuffer(RailInfo info) {
		
		if (!buffers.containsKey(RailRenderUtil.renderID(info))) {
			// Get model for current state
			final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
			
			IBlockState gravelState = Blocks.GRAVEL.getDefaultState();
			IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
			
			// Create render targets
			BufferBuilder worldRenderer = new BufferBuilder(2048);
	
			// Start drawing
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
	
			// From IE
			worldRenderer.color(255, 255, 255, 255);
			
			// This is evil but really fast :D
			for (TrackBase base : info.getBuilder().getTracksForRender()) {
				blockRenderer.getBlockModelRenderer().renderModel(info.world, new ScaledModel(gravelModel, base.getHeight()), gravelState, base.getPos(), worldRenderer, false);
			}
			
			worldRenderer.finishDrawing();
			
			buffers.put(RailRenderUtil.renderID(info), worldRenderer);
		}
		
		return buffers.get(RailRenderUtil.renderID(info));
	}
}
