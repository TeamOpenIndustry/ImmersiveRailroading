package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class RailItemRender implements IBakedModel {
	private static OBJRender baseRailModel;
	private static List<String> groups;

	static {
		try {
			baseRailModel = new OBJRender(new OBJModel(new ResourceLocation(ImmersiveRailroading.MODID, "models/block/track_1m.obj"), 0.05f));
			groups = new ArrayList<String>();
			
			for (String groupName : baseRailModel.model.groups())  {
				if (groupName.contains("RAIL_LEFT")) {
					groups.add(groupName);
				}
			}
		} catch (Exception e) {
			ImmersiveRailroading.catching(e);
		}
	}


	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		GL11.glPushMatrix();
		{
			GL11.glTranslated(0, 0.2, 0.55);
			baseRailModel.bindTexture();
			baseRailModel.drawGroups(groups);
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
