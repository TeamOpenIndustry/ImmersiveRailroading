package cam72cam.immersiverailroading.render.multiblock;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MBBlueprintRender {
	private static BlockRendererDispatcher blockRenderer;

	public static void draw(World world, String name) {
		if (blockRenderer == null) {
			blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		}
		
		// Create render targets
		BufferBuilder worldRenderer = new BufferBuilder(2048);

		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		worldRenderer.color(255, 255, 255, 255);
		
		Multiblock mb = MultiblockRegistry.get(name);
		if (mb == null) {
			// Some wrappers (Akashic Tome) remove the metadata
			return;
		}
		Map<BlockPos, IBlockState> bp = mb.blueprint();
		for (BlockPos pos : bp.keySet()) {
			
			IBlockState state = bp.get(pos);
			if (state == null || state.getBlock() == Blocks.AIR) {
				continue;
			}
			
			IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);
			blockRenderer.getBlockModelRenderer().renderModel(world, model, state, pos.subtract(mb.placementPos()), worldRenderer, false);
		}
		
		worldRenderer.finishDrawing();

		RailRenderUtil.draw(worldRenderer);
	}
}
