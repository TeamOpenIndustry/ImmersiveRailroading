package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.render.VBA;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.render.OBJRender;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class RailItemRender implements IBakedModel {
	private static OBJRender baseRailModel;
	private static List<String> left;

	static {
		try {
			baseRailModel = StockRenderCache.getTrackRenderer(DefinitionManager.getTracks().stream().findFirst().get().getTrackForGauge(0));
			List<String> groups = new ArrayList<String>();
			
			for (String groupName : baseRailModel.model.groups())  {
				if (groupName.contains("RAIL_LEFT")) {
					groups.add(groupName);
				}
			}
			left = groups;
		} catch (Exception e) {
			ImmersiveRailroading.catching(e);
		}
	}


	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		GL11.glPushMatrix();
		{
			GL11.glTranslated(0.5, 0.2, 0.55);
			baseRailModel.bindTexture();
			baseRailModel.drawGroups(left);
			baseRailModel.restoreTexture();
		}
		GL11.glPopMatrix();
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
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(Blocks.IRON_BLOCK.getDefaultState()).getParticleTexture();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}
}
