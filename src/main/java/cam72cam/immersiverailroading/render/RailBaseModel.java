package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.blocks.BlockRailBase.RailBlockState;
import cam72cam.immersiverailroading.render.rail.ScaledModel;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class RailBaseModel implements IBakedModel {
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (state instanceof RailBlockState) {
			RailBlockState railState = ((RailBlockState)state);
			if (railState.snow != 0) {
				state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, railState.snow);
				IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
				return model.getQuads(state, side, rand);
			} else if (railState.bed.getItem() != Items.AIR) {
				ItemStack item = railState.bed;
				state = BlockUtil.itemToBlockState(item);
				IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
				return new ScaledModel(model, railState.height).getQuads(state, side, rand);
			}
		}
		return new ArrayList<BakedQuad>();
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return true;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(Blocks.IRON_BLOCK.getDefaultState()).getParticleTexture();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return null;
	}
}
