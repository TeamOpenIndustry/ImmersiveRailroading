package cam72cam.immersiverailroading.entity.registry;

import java.nio.ByteBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;

public class EntityRollingStockRender extends Render<EntityRollingStock> {

	private BufferBuilder buffer;

	protected EntityRollingStockRender(RenderManager renderManager) {
		super(renderManager);
	}
	
	public boolean shouldRender(EntityRollingStock livingEntity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(EntityRollingStock entity, double x, double y, double z, float entityYaw, float partialTicks) {
		if (buffer == null)
		{
			System.out.println("WOOOO");
			ResourceLocation modelLoc = new ResourceLocation(ImmersiveRailroading.MODID, "models/rolling_stock/ModelLocoSteamShay.obj");
			IModel model = null;
			try {
				model = OBJLoader.INSTANCE.loadModel(modelLoc);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//Minecraft.getMinecraft().getResourceManager().getResource(modelLoc).getInputStream();
			
			OBJModel m = (OBJModel)model;
			Builder<String, String> q = ImmutableMap.builder();
			q.put("flip-v", "true");
			q.put("ambient", "true");
			ImmutableMap<String, String> customData = q.build();
			m = (OBJModel) m.process(customData);
			IBakedModel baked = m.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
			buffer = getModelBuffer(entity, baked);
		}
		
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		// Bind block textures to current context
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		// From IE
		RenderHelper.disableStandardItemLighting();
		
		GlStateManager.color(1, 1, 1);

		// Move to specified position
		GlStateManager.translate(x, y, z);
		
		// Finish Drawing
		draw(buffer);

		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
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
	
	protected BufferBuilder getModelBuffer(EntityRollingStock e, IBakedModel model) {
		
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

	@Override
	protected ResourceLocation getEntityTexture(EntityRollingStock entity) {
		return null;
	}
}
