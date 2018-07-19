package cam72cam.immersiverailroading.render.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.render.BakedScaledModel;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;

public class RailBaseModel implements IBakedModel {
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (state instanceof IExtendedBlockState) {
			IExtendedBlockState railState = (IExtendedBlockState)state;
			ItemStack bed = railState.getValue(BlockRailBase.RAIL_BED);
			if (bed != null) { // wait for tile to be initialized
				float height = railState.getValue(BlockRailBase.HEIGHT).floatValue();
				float tileHeight = height;
				int snow = railState.getValue(BlockRailBase.SNOW).intValue();
				int daysUntouched = railState.getValue(BlockRailBase.DAYS_UNTOUCHED).intValue();
				Augment augment = railState.getValue(BlockRailBase.AUGMENT);
				double gauged = railState.getValue(BlockRailBase.GAUGE).doubleValue();
				double liquid = railState.getValue(BlockRailBase.LIQUID);
				EnumFacing facing = railState.getValue(BlockRailBase.FACING);
				Gauge gauge = Gauge.from(gauged);
				
				if (augment != null) {
					height = height + 0.1f * (float)gauge.scale() * 1.25f;

					state = Blocks.CONCRETE.getDefaultState();
					state = state.withProperty(BlockColored.COLOR, augment.tempColor());
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					List<BakedQuad> quads = new ArrayList<BakedQuad>();
					quads.addAll(new BakedScaledModel(model, height).getQuads(state, side, rand));
					
					if (augment == Augment.WATER_TROUGH && facing != null) {
						Vec3d scale = new Vec3d(1, height, 0.5 * gauge.scale());
						Vec3d pos = new Vec3d(0,height,0.5-0.25 * gauge.scale());
						if (facing.getAxis() == Axis.Z) {
							scale = new Vec3d(0.5 * gauge.scale(), height, 1);
							pos = new Vec3d(0.5-0.25 * gauge.scale(),height,0);
						}
						
						if (side != EnumFacing.DOWN && side != EnumFacing.UP) {
							state = state.withProperty(BlockColored.COLOR, EnumDyeColor.GRAY);
							BakedScaledModel container = new BakedScaledModel(model, scale, pos);
							quads.addAll(container.getQuads(state, side, rand));
						} else if (liquid > 0) {
							state = state.withProperty(BlockColored.COLOR, EnumDyeColor.BLUE);
							scale = new Vec3d(scale.x, scale.y * liquid, scale.z);
							IBakedModel water = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
							IBakedModel waterScaled = new BakedScaledModel(water, scale, pos);
							quads.addAll(waterScaled.getQuads(state, side, rand));
						}
					}
					
					return quads;
				}
				
				height = height + 0.1f * (float)gauge.scale();
				
				if (snow != 0) {
					state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snow);
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					return model.getQuads(state, side, rand);
				} else if (bed.getItem() != Items.AIR && tileHeight != 0.000001f) {
					List<BakedQuad> quads = new ArrayList<BakedQuad>();
					ItemStack item = bed;
					state = BlockUtil.itemToBlockState(item);
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
					quads.addAll(new BakedScaledModel(model, height).getQuads(state, side, rand));
					Random rnd = new Random();
					int random = rnd.nextInt(2);
					if (random == 1) {
						IBlockState grassState = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
						quads.addAll(Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(grassState).getQuads(grassState, side, rand));
					}
					return quads;
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
