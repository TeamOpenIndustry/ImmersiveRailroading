package cam72cam.immersiverailroading.render.rail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

public class ScaledModel implements IBakedModel {
	// I know this is evil and I love it :D
	
	private IBakedModel source;
	private float height;
	
	public ScaledModel(IBakedModel source, float height) {
		this.source = source;
		this.height = height + 0.1f;
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		Minecraft.getMinecraft().mcProfiler.startSection("hack");
		List<BakedQuad> quads = source.getQuads(state, side, rand);
		List<BakedQuad> newQuads = new ArrayList<BakedQuad>();
		for (BakedQuad quad : quads) {
			int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

            VertexFormat format = quad.getFormat();
			
			for (int i = 0; i < 4; ++i)
	        {
				int j = format.getIntegerSize() * i;
	            newData[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 1]) * height);
	        }
			
			newQuads.add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
		}

		Minecraft.getMinecraft().mcProfiler.endSection();;
		
		return newQuads;
	}

	@Override
	public boolean isAmbientOcclusion() { return source.isAmbientOcclusion(); }
	@Override
	public boolean isGui3d() { return source.isGui3d(); }
	@Override
	public boolean isBuiltInRenderer() { return source.isBuiltInRenderer(); }
	@Override
	public TextureAtlasSprite getParticleTexture() { return source.getParticleTexture(); }
	@Override
	public ItemOverrideList getOverrides() { return source.getOverrides(); }
	
}