package cam72cam.immersiverailroading.render.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.items.nbt.ItemTrackExchanger;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.render.OBJRender;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class TrackExchangerModel implements IBakedModel {
	private static OBJRender RENDERER;
	private static RailInfo INFO = new RailInfo(null, 
												new RailSettings(Gauge.from(Gauge.STANDARD), "", TrackItems.STRAIGHT, 18, 0, TrackPositionType.FIXED, TrackDirection.NONE, new ItemStack(Items.AIR), new ItemStack(Items.AIR), false, false), 
												new PlacementInfo(new Vec3d(0, 0, 0), TrackDirection.NONE, 0, new Vec3d(0, 0, 0)),
												null,
												SwitchState.NONE,
												SwitchState.NONE,
												0);
	
	private RailInfo info;
	private RailInfo info2;
	private OBJRender renderer;

	public TrackExchangerModel() {}
	
	public TrackExchangerModel(ItemStack stack, World world) {
		if (RENDERER == null) {
			try {
				RENDERER = new OBJRender(new OBJModel(new ResourceLocation("immersiverailroading:models/item/track_exchanger/track_exchanger.obj"), -0.05f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (world == null) world = Minecraft.getMinecraft().world;
		
		this.renderer = RENDERER;
		String track = ItemTrackExchanger.get(stack);
		this.info = INFO.withTrack(track);
		this.info2 = null;
		if (Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
			TileRailBase railSlave = TileRailBase.get(world, Minecraft.getMinecraft().objectMouseOver.getBlockPos());
			if (railSlave != null) {
				TileRail rail = railSlave.getParentTile();
				if (rail != null) {
					this.info2 = INFO.withTrack(rail.info.settings.track);
				}
			}
		}
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (info == null) {
			return new ArrayList<BakedQuad>();
		}
	
		if (this.renderer != null) {
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, this.renderer.hasTexture());
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);

			GL11.glPushMatrix();
			this.renderer.bindTexture();
			this.renderer.draw();
			this.renderer.restoreTexture();
			
			GL11.glScaled(0.01, 0.01, 0.01);
			GL11.glRotated(90, 1, 0, 0);
			
			GL11.glTranslated(-15.15, 0.75, -8.75);
			RailBaseRender.draw(this.info);
			RailBuilderRender.renderRailBuilder(this.info);
			
			if (this.info2 != null) {
				GL11.glTranslated(-22.05, 0, 0);
				RailBaseRender.draw(this.info2);
				RailBuilderRender.renderRailBuilder(this.info2);
			}
			
			GL11.glPopMatrix();
			
			tex.restore();
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
			return new TrackExchangerModel(stack, world);
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
			return Pair.of(defaultVal.getLeft(), new Matrix4().scale(1.5, 1.5, 1.5).translate(0.5, 0.5, 0.5).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-20), 0, 1, 0).scale(2, 2, 2).translate(0.7, 0.65, 0.5).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().scale(2, 2, 2).rotate(Math.toRadians(-90), 1, 0, 0).translate(0.75, 0.5, 0.5).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().scale(2.5, 2.5, 2.5).translate(0.75, 0.5, 0).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().translate(0, 0, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
