package cam72cam.immersiverailroading.render;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class StockItemModel implements IBakedModel {

	private OBJModel model;
	private Matrix4 defaultTransform;

	public StockItemModel(EntityRollingStockDefinition def) {
		this.model = def.getModel();
		defaultTransform = def.getDefaultTransformation();
	}
	
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		/*
		 * I am an evil wizard!
		 * 
		 * So it turns out that I can stick a draw call in here to
		 * render my own stuff. This subverts forge's entire baked model
		 * system with a single line of code and injects my own OpenGL
		 * payload. Fuck you modeling restrictions.
		 * 
		 * This is probably really fragile if someone calls getQuads
		 * before actually setting up the correct GL context.
		 */
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		model.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
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

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		switch (cameraTransformType) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().scale(0.2, 0.2, 0.2).rotate(Math.toRadians(60), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().scale(0.2, 0.2, 0.2).rotate(Math.toRadians(10), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), defaultTransform.copy().toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(2, 2, 2).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0, -0.1, 0).scale(0.15, 0.15, 0.15).rotate(Math.toRadians(200), 0, 1, 0)
					.rotate(Math.toRadians(-15), 1, 0, 0).multiply(defaultTransform.copy()).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().translate(0, 0, 0.5).leftMultiply(defaultTransform).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
