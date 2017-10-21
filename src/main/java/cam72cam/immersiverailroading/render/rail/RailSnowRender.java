package cam72cam.immersiverailroading.render.rail;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class RailSnowRender {
	private static Map<String, BufferBuilder> snowBuffers = new HashMap<String, BufferBuilder>();
	
	private static String snowRenderID(RailInfo info) {
		return String.format("%s", info.position);
	}
	
	public static BufferBuilder getSnowBuffer(RailInfo info) {
		if (info.snowRenderFlagDirty) {
			if (snowBuffers.containsKey(snowRenderID(info))) {
				snowBuffers.remove(snowRenderID(info));
			}
		}
		
		if (!snowBuffers.containsKey(snowRenderID(info))) {
			final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
			BlockPos blockPos = info.position;
			
			IBlockState snowState = Blocks.SNOW_LAYER.getDefaultState();
			
			// Create render targets
			BufferBuilder worldRenderer = new BufferBuilder(2048);
	
			// Start drawing
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
	
			// From IE
			worldRenderer.color(255, 255, 255, 255);
			
			boolean hasSnow = false;
			
			// This is evil but really fast :D
			for (TrackBase base : info.getBuilder().getTracksForRender()) {
				TileEntity snowTe = info.world.getTileEntity(blockPos.add(base.getPos()));
				if (snowTe == null) {
					continue;
				}
				if (!(snowTe instanceof TileRailBase)) {
					continue;
				}
				int snowLevel = ((TileRailBase)snowTe).getSnowLayers();
				if (snowLevel == 0) {
					continue;
				}
				snowState = snowState.withProperty(BlockSnow.LAYERS, snowLevel);
				IBakedModel snowModel = blockRenderer.getBlockModelShapes().getModelForState(snowState);
				blockRenderer.getBlockModelRenderer().renderModel(info.world, new TranslatedModel(snowModel, base.getHeight()), snowState, base.getPos(), worldRenderer, false);
				hasSnow = true;
			}
			
			worldRenderer.finishDrawing();
			
			if (!hasSnow) {
				snowBuffers.put(snowRenderID(info), null);
			} else {
				snowBuffers.put(snowRenderID(info), worldRenderer);
			}
		}
		
		return snowBuffers.get(snowRenderID(info));
	}
}
