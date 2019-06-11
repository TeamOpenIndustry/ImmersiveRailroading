package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class LargeWrenchModel implements IBakedModel {
	private static OBJRender RENDERER;
	
	private OBJRender renderer;
	
	public LargeWrenchModel() {
		if (RENDERER == null) {
			try {
				RENDERER = new OBJRender(new OBJModel(new ResourceLocation("immersiverailroading:models/item/wrench/wrench.obj"), -0.5f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		this.renderer = RENDERER;
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (this.renderer != null) {
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			GLBoolTracker lighting = new GLBoolTracker(GL11.GL_LIGHTING, false);

			GL11.glPushMatrix();
			this.renderer.bindTexture();
			this.renderer.draw();
			this.renderer.restoreTexture();
			GL11.glPopMatrix();

			lighting.restore();
			cull.restore();
			
			this.renderer = null;
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
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return null;
	}
	
	public class ItemOverrideListHack extends ItemOverrideList {
		public ItemOverrideListHack() {
			super(new ArrayList<ItemOverride>());
		}

		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
			return new LargeWrenchModel();
		}
	}

	@Override
	public ItemOverrideList getOverrides() {
		return new ItemOverrideListHack();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		switch (cameraTransformType) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).translate(1.5, 0.375, 1.5).scale(3, 3, 3).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).rotate(Math.toRadians(-45), 0, 0, 1).translate(21, 1, 23.2).scale(35, 35, 35).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(90), 1, 0, 0).scale(4, 4, 4).translate(0.5, 0.1, 0.5).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(180), 0, 1, 0).rotate(Math.toRadians(-45), 0, 0, 1).scale(1.75, 1.75, 1.75).translate(0.5, 0.2, 0.5).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-45), 0, 0, 1).scale(1.25, 1.25, 1.25).translate(0.5, 0.2, 0).rotate(Math.toRadians(0), 0, 1, 0).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().translate(0, 0, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
