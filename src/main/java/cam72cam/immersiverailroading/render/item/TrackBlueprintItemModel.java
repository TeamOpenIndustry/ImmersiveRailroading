package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.library.TrackItems;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class TrackBlueprintItemModel implements IBakedModel {
	private RailInfo info;

	public TrackBlueprintItemModel() {
	}
	
	public TrackBlueprintItemModel(ItemStack stack, World world) {
		if (world == null) {
			world = Minecraft.getMinecraft().world;
		}
		info = new RailInfo(world, stack, new PlacementInfo(stack, 360-10, new BlockPos(0, 0, 0), 0.5f, 0.5f, 0.5f), null);
		info = info.withLength(10);
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (info == null) {
			return new ArrayList<BakedQuad>();
		}
		
		GL11.glPushMatrix();

		if (info.settings.type == TrackItems.TURN || info.settings.type == TrackItems.SWITCH) {
			GL11.glTranslated(0, 0, -0.1 * info.settings.quarters);
		}
		
		GL11.glRotated(-90, 0, 1, 0);
		GL11.glRotated(-90, 1, 0, 0);
		
		
			
		double scale = 0.95/info.settings.length;
		if (info.settings.type == TrackItems.CROSSING) {
			scale = 0.95 / 3;
		}
		if (info.settings.type == TrackItems.TURNTABLE) {
			scale *= 0.25;
		}
		GL11.glScaled(scale, -scale*2, scale);

		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
		GLBoolTracker lighting = new GLBoolTracker(GL11.GL_LIGHTING, false);

		GL11.glPushMatrix();
		{
			GL11.glTranslated(-0.5, 0, -0.5);
			RailBaseRender.draw(info);
		}
		GL11.glPopMatrix();
        RailBuilderRender.renderRailBuilder(info);

		lighting.restore();
		cull.restore();
		
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
		return null;
	}
	
	public class ItemOverrideListHack extends ItemOverrideList {
		public ItemOverrideListHack() {
			super(new ArrayList<ItemOverride>());
		}

		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
			return new TrackBlueprintItemModel(stack, world);
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
					new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).rotate(Math.toRadians(-60), 0, 0, 1).translate(0.5,0.25,0.5).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(90), 0, 1, 0).rotate(Math.toRadians(-30), 0, 0, 1).translate(0.5,0.25,0.5).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0.5,0,0.5).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0.5, 0, 0).rotate(Math.toRadians(+5+90), 0, 1, 0).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().translate(0, 0, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
