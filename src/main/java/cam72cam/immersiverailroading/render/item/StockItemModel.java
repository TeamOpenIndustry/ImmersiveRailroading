package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemTextureVariant;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ItemLayerModel;
import util.Matrix4;

public class StockItemModel implements IBakedModel {
	private OBJRender model;
	private double scale;
	private String defID;
	private ImmutableList<BakedQuad> iconQuads;
	private String texture;

	public StockItemModel() {
	}
	
	public StockItemModel(ItemStack stack) {
		scale = ItemGauge.get(stack).scale();
		defID = ItemDefinition.getID(stack);
		model = StockRenderCache.getRender(defID);
		if (model == null) {
			stack.setCount(0);
		}
		iconQuads = null;
		texture = ItemTextureVariant.get(stack);
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
		
		
		if (ConfigGraphics.enableIconCache) {
			if (iconQuads != null) {
				return iconQuads.asList();
			}
		}
		
		if (model != null) {
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, model.hasTexture());
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			
			GL11.glPushMatrix();
			double scale = 0.2 * Math.sqrt(this.scale);
			GL11.glScaled(scale, scale, scale);
			model.bindTexture(texture);
			model.draw();
			model.restoreTexture();
			GL11.glPopMatrix();
			
			tex.restore();
			cull.restore();
			
			// Model can only be rendered once.  If mods go through the itemrenderer as they are supposed to this should work just fine
			model = null;
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
			return new StockItemModel(stack);
		}
	}

	@Override
	public ItemOverrideList getOverrides() {
		return new ItemOverrideListHack();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		
		if (ConfigGraphics.enableIconCache && this.defID != null) {
			if (iconQuads == null) {
				TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
				TextureAtlasSprite sprite = map.getAtlasSprite(new ResourceLocation(ImmersiveRailroading.MODID, defID).toString());
				if (!sprite.equals(map.getMissingSprite())) {					
					iconQuads = ItemLayerModel.getQuadsForSprite(-1, sprite, DefaultVertexFormats.ITEM, Optional.empty());
				}
			}
			if (iconQuads != null) {
				return defaultVal;
			}
		}
		
		switch (cameraTransformType) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(60), 1, 0, 0).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(10), 1, 0, 0).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(-0.5, 0.25, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0.5, 0.25, 0.5).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0.5, 0, 0).rotate(Math.toRadians(+5+90), 0, 1, 0).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().scale(2,2,2).translate(-0.5, 0.6, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
