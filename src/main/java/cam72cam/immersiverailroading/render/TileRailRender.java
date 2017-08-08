package cam72cam.immersiverailroading.render;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderBase.VecYawPitch;
import cam72cam.immersiverailroading.track.TrackBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class TileRailRender extends TileEntitySpecialRenderer<TileRail> {
	
	private static OBJModel baseRailModel;
	
	static {
		try {
			baseRailModel = new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m.obj"));
		} catch (Exception e) {
			ImmersiveRailroading.logger.catching(e);
		}
	}
	
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}
	
	private static Map<String, Integer> displayLists = new HashMap<String, Integer>();

	@Override
	public void render(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!te.isVisible()) {
			return;
		}
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		// Bind block textures to current context
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		// From IE
		RenderHelper.disableStandardItemLighting();

		// Move to specified position
		GlStateManager.translate(x, y, z);
		
		// Finish Drawing
		draw(getBaseBuffer(te));
		
		RenderHelper.enableStandardItemLighting();

		
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		

		if (!displayLists.containsKey(renderID(te))) {
			int displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			
			switch (te.getFacing().getOpposite()) {
			case EAST:
				GlStateManager.translate(0, 0, 1);
				break;
			case NORTH:
				GlStateManager.translate(1, 0, 1);
				break;
			case SOUTH:
				// No Change
				break;
			case WEST:
				GlStateManager.translate(1, 0, 0);
				break;
			default:
				break;
			}
			BuilderBase builder = te.getType().getBuilder(te.getWorld(), new BlockPos(0,0,0), te.getFacing().getOpposite());
			for (VecYawPitch piece : builder.getRenderData()) {
				GlStateManager.pushMatrix();
				GlStateManager.rotate(180-te.getFacing().getHorizontalAngle(), 0, 1, 0);
				GlStateManager.translate(piece.x, piece.y, piece.z);
				GlStateManager.rotate(piece.getPitch(), 1, 0, 0);
				GlStateManager.rotate(piece.getYaw(), 0, 1, 0);
				GlStateManager.rotate(-90, 0, 1, 0);
				baseRailModel.drawDirect();
				GlStateManager.popMatrix();
			}

			GL11.glEndList();
			
			displayLists.put(renderID(te), displayList);
		}
		
		GL11.glCallList(displayLists.get(renderID(te)));
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
	
	/*
	 *  From WorldVertexBufferUploader.draw
	 *  
	 *  Excludes the reset buffer at the end
	 */
	private void draw(BufferBuilder vertexBufferIn) {
        VertexFormat vertexformat = vertexBufferIn.getVertexFormat();
        int i = vertexformat.getNextOffset();
        ByteBuffer bytebuffer = vertexBufferIn.getByteBuffer();
        List<VertexFormatElement> list = vertexformat.getElements();

        for (int j = 0; j < list.size(); ++j)
        {
            VertexFormatElement vertexformatelement = list.get(j);
            bytebuffer.position(vertexformat.getOffset(j));

            // moved to VertexFormatElement.preDraw
            vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
        }

        GlStateManager.glDrawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount());
        int i1 = 0;

        for (int j1 = list.size(); i1 < j1; ++i1)
        {
            VertexFormatElement vertexformatelement1 = list.get(i1);
            // moved to VertexFormatElement.postDraw
            vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
        }
	}
	
	

	
	private static Map<String, BufferBuilder> buffers = new HashMap<String, BufferBuilder>();
	private static String renderID(TileRail te) {
		//TODO more attributes like railbed
		return String.format("%s%s", te.getFacing(), te.getType());
	}
	
	/*
	 * This returns a cached buffer as rails don't change their model often
	 * This drastically reduces the overhead of rendering these complex models
	 * 
	 * We also draw the railbed here since drawing a model for each gag eats FPS 
	 */
	protected static BufferBuilder getBaseBuffer(TileRail te) {
		
		if (!buffers.containsKey(renderID(te))) {
			// Get model for current state
			final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
			BlockPos blockPos = te.getPos();
			/*
			IBlockState state = te.getWorld().getBlockState(blockPos);
			state = te.getBlockState();
			IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);
			*/
			
			IBlockState gravelState = Blocks.GRAVEL.getDefaultState();
			IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
			
			// Create render targets
			BufferBuilder worldRenderer = new BufferBuilder(2048);
	
			// Reverse position which will be done render model
			worldRenderer.setTranslation(-blockPos.getX(), -blockPos.getY(), -blockPos.getZ());
	
			// Start drawing
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
	
			// From IE
			worldRenderer.color(255, 255, 255, 255);
	
			// Render block at position
			//blockRenderer.getBlockModelRenderer().renderModel(te.getWorld(), model, state, blockPos, worldRenderer, false);
			
			// This is evil but really fast :D
			BuilderBase builder = te.getType().getBuilder(te.getWorld(), new BlockPos(0,0,0), te.getFacing().getOpposite());
			for (TrackBase base : builder.getTracks()) {
				blockRenderer.getBlockModelRenderer().renderModel(te.getWorld(), new ScaledModel(gravelModel, base.getHeight()), gravelState, blockPos.add(base.getPos()), worldRenderer, false);
			}
			//Debug center location
			if (te.getCenter() != null) {
				blockRenderer.getBlockModelRenderer().renderModel(te.getWorld(), new ScaledModel(gravelModel, te.getHeight()), gravelState, new BlockPos(te.getCenter()), worldRenderer, false);
			}
			
			worldRenderer.finishDrawing();
			
			buffers.put(renderID(te), worldRenderer);
		}
		
		return buffers.get(renderID(te));
	}
}
