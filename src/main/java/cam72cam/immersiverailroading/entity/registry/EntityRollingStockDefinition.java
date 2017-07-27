package cam72cam.immersiverailroading.entity.registry;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.render.obj.OBJModel;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public abstract class EntityRollingStockDefinition {
	public abstract EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing);

	protected String defID;
	private String name;
	private OBJModel model;
	private Vec3d passengerDefault;
	private float bogeyFront;
	private float bogeyRear;

	private Matrix4 defaultTransform = new Matrix4();

	private double frontBounds;
	private double rearBounds;
	private double heightBounds;
	private double widthBounds;
	private double passengerMaxFront;
	private double passengerMaxRear;
	private double passengerMaxWidth;

	public EntityRollingStockDefinition(String defID, JsonObject data) throws Exception {
		this.defID = defID;

		name = data.get("name").getAsString();
		// model = (OBJModel) OBJLoader.INSTANCE.loadModel(new
		// ResourceLocation(data.get("model").getAsString()));
		model = new OBJModel(new ResourceLocation(data.get("model").getAsString()));
		JsonObject passenger = data.get("passenger").getAsJsonObject();
		passengerDefault = new Vec3d(passenger.get("default_x").getAsDouble(), passenger.get("default_y").getAsDouble(),
				passenger.get("default_z").getAsDouble());
		passengerMaxFront = passenger.get("front").getAsDouble();
		passengerMaxRear = passenger.get("rear").getAsDouble();
		passengerMaxWidth = passenger.get("width").getAsDouble();

		bogeyFront = data.get("trucks").getAsJsonObject().get("front").getAsFloat();
		bogeyRear = data.get("trucks").getAsJsonObject().get("rear").getAsFloat();

		JsonObject rotations = data.get("rotate").getAsJsonObject();
		if (rotations.has("x")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("x").getAsFloat()), 1, 0, 0);
		}
		if (rotations.has("y")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("y").getAsFloat()), 0, 1, 0);
		}
		if (rotations.has("z")) {
			defaultTransform.rotate(Math.toRadians(rotations.get("z").getAsFloat()), 0, 0, 1);
		}

		JsonObject boundsData = data.get("bounds").getAsJsonObject();
		frontBounds = boundsData.get("front").getAsDouble();
		rearBounds = boundsData.get("rear").getAsDouble();
		widthBounds = boundsData.get("width").getAsDouble();
		heightBounds = boundsData.get("height").getAsDouble();
	}

	public void render(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks) {
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();

		// Bind block textures to current context
		if (model.texLoc != null) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(model.texLoc);
		}

		// From IE
		RenderHelper.disableStandardItemLighting();

		GlStateManager.color(1, 1, 1);

		// Move to specified position
		GlStateManager.translate(x, y, z);

		GlStateManager.scale(2, 2, 2);

		GlStateManager.rotate(180 - entityYaw, 0, 1, 0);
		FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
		Matrix4 transform = defaultTransform.copy().rotate(Math.toRadians(180), 0, 1, 0);
		matrix.put((float) transform.m00);
		matrix.put((float) transform.m01);
		matrix.put((float) transform.m02);
		matrix.put((float) transform.m03);
		matrix.put((float) transform.m10);
		matrix.put((float) transform.m11);
		matrix.put((float) transform.m12);
		matrix.put((float) transform.m13);
		matrix.put((float) transform.m20);
		matrix.put((float) transform.m21);
		matrix.put((float) transform.m22);
		matrix.put((float) transform.m23);
		matrix.put((float) transform.m30);
		matrix.put((float) transform.m31);
		matrix.put((float) transform.m32);
		matrix.put((float) transform.m33);
		matrix.flip();

		GlStateManager.multMatrix(matrix);

		model.draw();

		// Finish Drawing
		// draw(getBuffer());
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();

		// No idea why I need the +1 here
		// Render.renderOffsetAABB(stock.getCollisionBoundingBox(), x, y, z);
	}

	public IBakedModel getInventoryModel() {
		return new IBakedModel() {
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
				if (model.texLoc != null) {
					Minecraft.getMinecraft().getTextureManager().bindTexture(model.texLoc);
				} else {
					Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_MISSING_TEXTURE);
				}
				model.draw();
				Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
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
							new Matrix4().scale(0.4, 0.4, 0.4).rotate(Math.toRadians(60), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
				case FIRST_PERSON_LEFT_HAND:
				case FIRST_PERSON_RIGHT_HAND:
					return Pair.of(defaultVal.getLeft(),
							new Matrix4().scale(0.4, 0.4, 0.4).rotate(Math.toRadians(10), 1, 0, 0).multiply(defaultTransform).toMatrix4f());
				case GROUND:
					return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(2, 2, 2).toMatrix4f());
				case FIXED:
					// Item Frame
					return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(4, 4, 4).toMatrix4f());
				case GUI:
					return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0, -0.1, 0).scale(0.3, 0.3, 0.3).rotate(Math.toRadians(200), 0, 1, 0)
							.rotate(Math.toRadians(-15), 1, 0, 0).multiply(defaultTransform.copy()).toMatrix4f());
				case HEAD:
					return Pair.of(defaultVal.getLeft(),
							new Matrix4().scale(2, 2, 2).translate(0, 0, 0.5).leftMultiply(defaultTransform).toMatrix4f());
				case NONE:
					return defaultVal;
				}
				return defaultVal;
			}
		};
	}

	public Vec3d getPlayerOffset() {
		return this.passengerDefault;
	}
	public Vec3d correctPassengerBounds(Vec3d pos) {
		if (pos.x > this.passengerMaxFront) {
			pos = new Vec3d(this.passengerMaxFront, pos.y, pos.z);
		}
		
		if (pos.x < this.passengerMaxRear) {
			pos = new Vec3d(this.passengerMaxRear, pos.y, pos.z);
		}
		
		if (Math.abs(pos.z+0.75) > this.passengerMaxWidth/2) {
			pos = new Vec3d(pos.x, pos.y, Math.copySign(this.passengerMaxWidth/2, pos.z)-0.75);
		}
		
		return pos;
	}

	public float getBogeyFront() {
		return this.bogeyFront;
	}

	public float getBogeyRear() {
		return this.bogeyRear;
	}
	
	public double getCouplerPosition(CouplerType coupler) {
		switch(coupler) {
		case FRONT:
			return this.frontBounds + Config.couplerRange;
		case BACK:
			return this.rearBounds + Config.couplerRange;
		default:
			return 0;
		}
	}

	public AxisAlignedBB getBounds(EntityMoveableRollingStock stock) {
		return new RealBB(frontBounds, rearBounds, widthBounds, heightBounds, stock.rotationYaw).offset(stock.getPositionVector());
	}

	public List<String> getTooltip() {
		List<String> tips = new ArrayList<String>();
		return tips;
	}
}
