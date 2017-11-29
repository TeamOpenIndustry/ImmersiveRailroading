package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.Augment;
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
import net.minecraftforge.common.property.IExtendedBlockState;

public class RailBaseModel implements IBakedModel {
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (state instanceof IExtendedBlockState) {
			IExtendedBlockState railState = (IExtendedBlockState)state;
			ItemStack bed = railState.getValue(BlockRailBase.RAIL_BED);
			if (bed != null) { // wait for tile to be initialized
				float height = railState.getValue(BlockRailBase.HEIGHT).floatValue();
				int snow = railState.getValue(BlockRailBase.SNOW).intValue();
				Augment augment = railState.getValue(BlockRailBase.AUGMENT);
				//double gauge = railState.getValue(BlockRailBase.GAUGE).doubleValue();
				
				if (augment != null) {
					state = Blocks.IRON_BLOCK.getDefaultState();
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					return model.getQuads(state, side, rand);
				}
				
				if (snow != 0) {
					state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snow);
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					return model.getQuads(state, side, rand);
				} else if (bed.getItem() != Items.AIR) {
					ItemStack item = bed;
					state = BlockUtil.itemToBlockState(item);
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					return new ScaledModel(model, height).getQuads(state, side, rand);
				}
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
