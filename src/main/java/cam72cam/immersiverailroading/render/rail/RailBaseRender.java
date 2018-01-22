package cam72cam.immersiverailroading.render.rail;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.render.BakedScaledModel;
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
	private static BufferBuilder worldRenderer = new BufferBuilder(2048);
	
	/*
	 * This returns a cached buffer as rails don't change their model often
	 * This drastically reduces the overhead of rendering these complex models
	 * 
	 * We also draw the railbed here since drawing a model for each gag eats FPS 
	 */
	private static BufferBuilder getBaseBuffer(RailInfo info) {
		// Get model for current state
		final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		
		if (info.railBed.getItem() == Items.AIR) {
			return null;
		}
		IBlockState gravelState = BlockUtil.itemToBlockState(info.railBed);
		IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
		
		// Create render targets

		// Start drawing
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

		// From IE
		worldRenderer.color(255, 255, 255, 255);
		
		// This is evil but really fast :D
		for (TrackBase base : info.getBuilder().getTracksForRender()) {
			blockRenderer.getBlockModelRenderer().renderModel(info.world, new BakedScaledModel(gravelModel, base.getHeight() + 0.1f * (float)info.gauge.scale()), gravelState, base.getPos(), worldRenderer, false);
		}
		
		worldRenderer.finishDrawing();
		return worldRenderer;
	}
	
	private static synchronized void drawSync(RailInfo info) {
		RailRenderUtil.draw(getBaseBuffer(info));
	}

	private static Map<String, Integer> displayLists = new HashMap<String, Integer>();
	public static void draw(RailInfo info) {
		if (!displayLists.containsKey(RailRenderUtil.renderID(info))) {
			int displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			drawSync(info);
			GL11.glEndList();
			displayLists.put(RailRenderUtil.renderID(info), displayList);
		}
		GL11.glCallList(displayLists.get(RailRenderUtil.renderID(info)));
	}
}
