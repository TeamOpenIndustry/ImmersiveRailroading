package cam72cam.immersiverailroading.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.animation.FastTESR;

public class TileRailTESR extends FastTESR<TileRail> {
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}

	@Override
	public void renderTileEntityFast(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha, BufferBuilder buffer) {

		// Get model for current state
		final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		BlockPos blockPos = te.getPos();
		IBlockState state = te.getBlockState();
		IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);

		// Move to specified position
		buffer.setTranslation(x-blockPos.getX(), y-blockPos.getY(), z-blockPos.getZ());

		// Render block at position
		blockRenderer.getBlockModelRenderer().renderModel(te.getWorld(), model, state, blockPos, buffer, true);
	}
}