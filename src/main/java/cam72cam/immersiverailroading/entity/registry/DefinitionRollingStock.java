package cam72cam.immersiverailroading.entity.registry;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonObject;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.MoveableRollingStock;
import cam72cam.immersiverailroading.util.RealBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import util.Matrix4;

public abstract class DefinitionRollingStock {
	public abstract EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing);

	protected String defID;
	private String name;
	private String works;
	private OBJModel model;
	private Vec3d playerOffset;
	private float bogeyFront;
	private float bogeyRear;

	private Matrix4 defaultTransform = new Matrix4();

	private BufferBuilder buffer;
	private double frontBounds;
	private double rearBounds;
	private double heightBounds;
	private double widthBounds;
	
	public DefinitionRollingStock(String defID, JsonObject data) throws Exception {
		this.defID = defID;

		name = data.get("name").getAsString();
		works = data.get("works").getAsString();
		model = (OBJModel) OBJLoader.INSTANCE.loadModel(new ResourceLocation(data.get("model").getAsString()));
		JsonObject properties = data.get("properties").getAsJsonObject();
		playerOffset = new Vec3d(properties.get("passenger_offset_x").getAsDouble(), properties.get("passenger_offset_y").getAsDouble(),
				properties.get("passenger_offset_z").getAsDouble());
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

	private IBakedModel getBakedModel() {
		Builder<String, String> q = ImmutableMap.builder();
		q.put("flip-v", "true");
		q.put("ambient", "true");
		ImmutableMap<String, String> customData = q.build();
		model = (OBJModel) model.process(customData);
		IBakedModel baked = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		return baked;
	}

	private BufferBuilder getBuffer() {
		// TODO rewrite this so we can have animations

		if (buffer == null) {

			buffer = buildBuffer(getBakedModel());
		}
		return buffer;
	}

	private BufferBuilder buildBuffer(IBakedModel model) {

		// Create render targets
		BufferBuilder worldRenderer = new BufferBuilder(2097152);

		// Start drawing
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);

		// From IE
		worldRenderer.color(255, 255, 255, 255);

		List<BakedQuad> quads = model.getQuads((IBlockState) null, (EnumFacing) null, 0L);
		int i = 0;
		for (int j = quads.size(); i < j; ++i) {
			net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(worldRenderer, quads.get(i), 0xFFFFFFFF);
		}

		worldRenderer.finishDrawing();

		return worldRenderer;
	}

	private void draw(BufferBuilder vertexBufferIn) {
		VertexFormat vertexformat = vertexBufferIn.getVertexFormat();
		int i = vertexformat.getNextOffset();
		ByteBuffer bytebuffer = vertexBufferIn.getByteBuffer();
		List<VertexFormatElement> list = vertexformat.getElements();

		for (int j = 0; j < list.size(); ++j) {
			VertexFormatElement vertexformatelement = list.get(j);
			bytebuffer.position(vertexformat.getOffset(j));

			// moved to VertexFormatElement.preDraw
			vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
		}

		GlStateManager.glDrawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount());
		int i1 = 0;

		for (int j1 = list.size(); i1 < j1; ++i1) {
			VertexFormatElement vertexformatelement1 = list.get(i1);
			// moved to VertexFormatElement.postDraw
			vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
		}
	}

	public void render(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks) {
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();

		// Bind block textures to current context
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

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

		// Finish Drawing
		draw(getBuffer());
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
		

		// No idea why I need the +1 here
		//Render.renderOffsetAABB(stock.getCollisionBoundingBox(), x, y, z);
	}

	public Collection<ResourceLocation> getTextures() {
		return model.getTextures();
	}

	public IBakedModel getInventoryModel() {
		IBakedModel m = getBakedModel();
		return new IBakedModel() {
			@Override
			public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
				// TODO Auto-generated method stub
				return m.getQuads(state, side, rand);
			}

			@Override
			public boolean isAmbientOcclusion() {
				return m.isAmbientOcclusion();
			}

			@Override
			public boolean isGui3d() {
				return m.isGui3d();
			}

			@Override
			public boolean isBuiltInRenderer() {
				return m.isBuiltInRenderer();
			}

			@Override
			public TextureAtlasSprite getParticleTexture() {
				return m.getParticleTexture();
			}

			@Override
			public ItemOverrideList getOverrides() {
				return m.getOverrides();
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
				case FIXED:
					return Pair.of(defaultVal.getLeft(), defaultTransform.copy().scale(2, 2, 2).toMatrix4f());
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
		return this.playerOffset;
	}

	public float getBogeyFront() {
		return this.bogeyFront;
	}

	public float getBogeyRear() {
		return this.bogeyRear;
	}
	
	public AxisAlignedBB getBounds(MoveableRollingStock stock) {
		return new RealBB(frontBounds, rearBounds, widthBounds, heightBounds, stock.rotationYaw).offset(stock.getPositionVector());
	}

	public List<String> getTooltip() {
		List<String> tips = new ArrayList<String>();
		tips.add("Works: " + this.works);
		return tips;
	}
}
