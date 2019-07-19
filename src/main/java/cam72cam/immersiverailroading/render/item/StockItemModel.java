package cam72cam.immersiverailroading.render.item;

import java.util.*;

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
import cam72cam.mod.render.obj.OBJRender;
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
	private static Map<String, ImmutableList<BakedQuad>> iconQuads = new HashMap<>();
	private String texture;

	public StockItemModel() {
	}
	
	public StockItemModel(ItemStack stack) {
		scale = ItemGauge.get(new cam72cam.mod.item.ItemStack(stack)).scale();
		defID = ItemDefinition.getID(new cam72cam.mod.item.ItemStack(stack));
		model = StockRenderCache.getRender(defID);
		if (model == null) {
			stack.setCount(0);
		}
		texture = ItemTextureVariant.get(new cam72cam.mod.item.ItemStack(stack));
	}
	
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

		
		if (ConfigGraphics.enableFlatIcons) {
			//if (iconQuads.get(defID) != null) {
			//	return iconQuads.get(defID).asList();
			//}


			TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
			TextureAtlasSprite sprite = map.getAtlasSprite(new ResourceLocation(ImmersiveRailroading.MODID, defID).toString());
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

			GL11.glPushMatrix();
			GL11.glRotated(180, 1, 0, 0);
			GL11.glTranslated(0, -1, 0);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glTexCoord2f(sprite.getMinU(), sprite.getMinV()); GL11.glVertex3f(0, 0, 0);
			GL11.glTexCoord2f(sprite.getMinU(), sprite.getMaxV()); GL11.glVertex3f(0, 1, 0);
			GL11.glTexCoord2f(sprite.getMaxU(), sprite.getMaxV()); GL11.glVertex3f(1, 1, 0);
			GL11.glTexCoord2f(sprite.getMaxU(), sprite.getMinV()); GL11.glVertex3f(1, 0, 0);
			GL11.glEnd();
			GL11.glPopMatrix();
			return new ArrayList<>();
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

	//@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspectivea(TransformType cameraTransformType) {
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		
		if (ConfigGraphics.enableFlatIcons && this.defID != null) {
			if (iconQuads.get(defID) == null) {
				// Might need to wipe iconQuads when a new texturesheet is loaded
				TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
				TextureAtlasSprite sprite = map.getAtlasSprite(new ResourceLocation(ImmersiveRailroading.MODID, defID).toString());
				if (!sprite.equals(map.getMissingSprite())) {					
					iconQuads.put(defID, ItemLayerModel.getQuadsForSprite(-1, sprite, DefaultVertexFormats.ITEM, Optional.empty()));
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
