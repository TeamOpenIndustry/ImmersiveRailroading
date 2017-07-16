package cam72cam.immersiverailroading.tile;

import java.nio.ByteBuffer;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class TileRailTESR extends TileEntitySpecialRenderer<TileRail> {
	@Override
	public boolean isGlobalRenderer(TileRail te) {
		return true;
	}

	@Override
	public void render(TileRail te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!te.isVisible()) {
			return;
		}
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		// Bind block textures to current context
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		// From IE
		RenderHelper.disableStandardItemLighting();

		// Move to specified position
		GlStateManager.translate(x, y, z);
		
		// Finish Drawing
		draw(te.getModelBuffer());

		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
	
	/*
	 *  From WorldVertexBufferUploader.draw
	 *  
	 *  Excludes the reset buffer at the end
	 */
	private void draw(BufferBuilder vertexBufferIn) {
        VertexFormat vertexformat = vertexBufferIn.getVertexFormat();
        int i = vertexformat.getNextOffset();
        ByteBuffer bytebuffer = vertexBufferIn.getByteBuffer();
        List<VertexFormatElement> list = vertexformat.getElements();

        for (int j = 0; j < list.size(); ++j)
        {
            VertexFormatElement vertexformatelement = list.get(j);
            VertexFormatElement.EnumUsage vertexformatelement$enumusage = vertexformatelement.getUsage();
            int k = vertexformatelement.getType().getGlConstant();
            int l = vertexformatelement.getIndex();
            bytebuffer.position(vertexformat.getOffset(j));

            // moved to VertexFormatElement.preDraw
            vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
        }

        GlStateManager.glDrawArrays(vertexBufferIn.getDrawMode(), 0, vertexBufferIn.getVertexCount());
        int i1 = 0;

        for (int j1 = list.size(); i1 < j1; ++i1)
        {
            VertexFormatElement vertexformatelement1 = list.get(i1);
            VertexFormatElement.EnumUsage vertexformatelement$enumusage1 = vertexformatelement1.getUsage();
            int k1 = vertexformatelement1.getIndex();

            // moved to VertexFormatElement.postDraw
            vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
        }
	}
}
