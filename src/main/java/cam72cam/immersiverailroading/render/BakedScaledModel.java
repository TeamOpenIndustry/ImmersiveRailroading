package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public class BakedScaledModel implements IBakedModel {
	// I know this is evil and I love it :D
	
	private IBakedModel source;
	private final Vec3d scale;
	private final Vec3d transform;
	private Map<EnumFacing, List<BakedQuad>> quadCache = new HashMap<EnumFacing, List<BakedQuad>>();
	
	public BakedScaledModel(IBakedModel source, Vec3d scale, Vec3d transform) {
		this.source = source;
		this.scale = scale;
		this.transform = transform;
	}
	
	public BakedScaledModel(IBakedModel source, float height) {
		this.source = source;
		this.scale = new Vec3d(1, height, 1);
		transform = new Vec3d(0,0,0);
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (quadCache.get(side) == null) {
			List<BakedQuad> quads = source.getQuads(state, side, rand);
			quadCache.put(side, new ArrayList<BakedQuad>());
			for (BakedQuad quad : quads) {
				int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);
	
	            VertexFormat format = quad.getFormat();
				
				for (int i = 0; i < 4; ++i)
		        {
					int j = format.getIntegerSize() * i;
		            newData[j + 0] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 0]) * (float)scale.x + (float)transform.x);
		            newData[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 1]) * (float)scale.y + (float)transform.y);
		            newData[j + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 2]) * (float)scale.z + (float)transform.z);
		        }
				
				quadCache.get(side).add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
			}
		}
		
		return quadCache.get(side);
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