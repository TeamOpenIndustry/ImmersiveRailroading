package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.StockRenderCache;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class StockItemComponentModel implements IBakedModel {
	private OBJRender renderer;
	private List<String> groups;
	private Vec3d center;
	private double width;
	private double length;
	private double scale;

	public StockItemComponentModel() {
	}
	
	public StockItemComponentModel(ItemStack stack) {
		scale = ItemGauge.get(stack).scale();
		String defID = ItemDefinition.getID(stack);
		ItemComponentType item = ItemComponent.getComponentType(stack);
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
		
		if (def == null) {
			ImmersiveRailroading.error("Item %s missing definition!", stack);
			stack.setCount(0);
			return;
		}
		
		renderer = StockRenderCache.getRender(defID);
		groups = new ArrayList<String>();

		for (RenderComponentType r : item.render) {
			RenderComponent comp = def.getComponent(r, Gauge.from(Gauge.STANDARD));
			if (comp == null || r == RenderComponentType.CARGO_FILL_X) {
				continue;
			}
			groups.addAll(comp.modelIDs);
		}
		
		center = renderer.model.centerOfGroups(groups);
		width = renderer.model.heightOfGroups(groups);
		length = renderer.model.lengthOfGroups(groups);
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (groups == null) {
			return new ArrayList<BakedQuad>();
		}
		
		GL11.glPushMatrix();
		
		//GL11.glRotated(-90, 0, 1, 0);
		//GL11.glRotated(90, 1, 0, 0);
		GL11.glTranslated(-center.x, -center.y, -center.z);
		
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, false);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, renderer.hasTexture());
		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		renderer.bindTexture();
		renderer.drawGroups(groups);
		renderer.restoreTexture();
		
		blend.restore();
		cull.restore();
		tex.restore();
		light.restore();
		
		GL11.glPopMatrix();
		
		// Model can only be rendered once.  If mods go through the itemrenderer as they are supposed to this should work just fine
		groups = null;
		
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
			return new StockItemComponentModel(stack);
		}
	}

	@Override
	public ItemOverrideList getOverrides() {
		return new ItemOverrideListHack();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {

		double scale = 1;
		if (width != 0 || length != 0) {
			scale = 0.95/Math.max(width, length);
		}
		scale *= Math.sqrt(this.scale);
		
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		switch (cameraTransformType) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).rotate(Math.toRadians(-60), 0, 0, 1).translate(0.5,0.25,0.5).scale(0.2, 0.2, 0.2).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).rotate(Math.toRadians(-30), 0, 0, 1).translate(0.5,0.25,0.5).scale(0.2, 0.2, 0.2).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0, 0.25, 0).scale(0.2, 0.2, 0.2).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().scale(scale, scale, scale).translate(0.5, 0.5, 0).toMatrix4f());
		case HEAD:
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
