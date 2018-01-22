package cam72cam.immersiverailroading.render.rail;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.render.BakedScaledModel;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;

public class RailBaseOverlayRender {
	private static BufferBuilder worldRenderer = new BufferBuilder(2048);
	
	private static BufferBuilder getOverlayBuffer(RailInfo info) {
		
		// Get model for current state
		final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		
		IBlockState gravelState = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED);
		IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
		
		// Create render targets
		
		worldRenderer.setTranslation(-info.position.getX(), -info.position.getY(), -info.position.getZ());

		// Start drawing
		try {
			System.err.println("DRAW: " + Thread.currentThread().getId());
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
	
			// From IE
			worldRenderer.color(255, 255, 255, 255);
			
			boolean hasIssue = false;
			
			// This is evil but really fast :D
			for (TrackBase base : info.getBuilder(info.position).getTracksForRender()) {
				if (!base.canPlaceTrack() ) {
					hasIssue = true;
					blockRenderer.getBlockModelRenderer().renderModel(info.world, new BakedScaledModel(gravelModel, base.getHeight()+0.2f), gravelState, base.getPos(), worldRenderer, false);
				}
			}
			
			if (!hasIssue) {
				return null;
			}
			
		} finally {
			worldRenderer.finishDrawing();
		}
		
		return worldRenderer;
	}

	public static synchronized void draw(RailInfo info) {
		RailRenderUtil.draw(RailBaseOverlayRender.getOverlayBuffer(info));
	}
}
