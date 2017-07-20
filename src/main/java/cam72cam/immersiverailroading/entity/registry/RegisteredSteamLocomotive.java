package cam72cam.immersiverailroading.entity.registry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.SteamLocomotive;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;

public class RegisteredSteamLocomotive implements IDefinitionRollingStock {
	private String name;
	private String works;
	private OBJModel model;
	private float bogeyFront;
	private float bogeyRear;
	private BufferBuilder buffer;
	private String defID;

	public RegisteredSteamLocomotive(String defID) throws Exception {
		this.defID = defID;

		ResourceLocation resource = new ResourceLocation(ImmersiveRailroading.MODID, defID);
		InputStream input = Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();

		JsonParser parser = new JsonParser();
		JsonObject result = parser.parse(new InputStreamReader(input)).getAsJsonObject();
		
		name = result.get("name").getAsString();
		works = result.get("works").getAsString();
		model = (OBJModel) OBJLoader.INSTANCE.loadModel(new ResourceLocation(result.get("model").getAsString()));
		bogeyFront = result.get("trucks").getAsJsonObject().get("front").getAsFloat();
		bogeyRear = result.get("trucks").getAsJsonObject().get("rear").getAsFloat();
	}

	@Override
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing) {
		SteamLocomotive loco = new SteamLocomotive(world, defID);
		
		loco.setPosition(pos.getX(), pos.getY(), pos.getZ());
		world.spawnEntity(loco);
		
		return loco;
	}

	private BufferBuilder getBuffer() {
		// TODO rewrite this so we can have animations

		if (buffer == null) {
			Builder<String, String> q = ImmutableMap.builder();
			q.put("flip-v", "true");
			q.put("ambient", "true");
			ImmutableMap<String, String> customData = q.build();
			model = (OBJModel) model.process(customData);
			IBakedModel baked = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
			buffer = buildBuffer(baked);
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
		
		List<BakedQuad> quads = model.getQuads((IBlockState)null, (EnumFacing)null, 0L);
		int i = 0;
		for (int j = quads.size(); i < j; ++i)
        {
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

        for (int j = 0; j < list.size(); ++j)
        {
            VertexFormatElement vertexformatelement = list.get(j);
            bytebuffer.position(vertexformat.getOffset(j));

            // moved to VertexFormatElement.preDraw
            vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
        }

        GlStateManager.glDrawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount());
        int i1 = 0;

        for (int j1 = list.size(); i1 < j1; ++i1)
        {
            VertexFormatElement vertexformatelement1 = list.get(i1);
            // moved to VertexFormatElement.postDraw
            vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
        }
	}

	@Override
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
		
		GlStateManager.rotate(180 - entityYaw+90, 0, 1, 0);
		
		// Finish Drawing
		draw(getBuffer());
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}

	@Override
	public void renderItem() {
		//TODO
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return model.getTextures();
	}
}
