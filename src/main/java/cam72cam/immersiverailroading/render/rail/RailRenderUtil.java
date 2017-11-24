package cam72cam.immersiverailroading.render.rail;

import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class RailRenderUtil {
	public static void render(RailInfo info, boolean renderOverlay) {
		GlStateManager.pushMatrix();
		
		// Bind block textures to current context
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		GLBoolTracker light = new GLBoolTracker(GL11.GL_LIGHTING, false);
		
		// Move to offset position
		//GL11.glTranslated(-info.getBuilder().getRenderOffset().getX(), 0, -info.getBuilder().getRenderOffset().getZ());
		GL11.glPushMatrix();

		GlStateManager.translate(-info.position.getX(), -info.position.getY(), -info.position.getZ());
		GlStateManager.translate(Math.floor(info.placementPosition.x), Math.floor(info.placementPosition.y), Math.floor(info.placementPosition.z));
		
		if (renderOverlay) {
			RailBaseRender.draw(info);
			RailRenderUtil.draw(RailBaseOverlayRender.getOverlayBuffer(info));
		}
		Minecraft.getMinecraft().mcProfiler.startSection("rail");
		
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		
		RailBuilderRender.renderRailBuilder(info);
		
		light.restore();
		
		Minecraft.getMinecraft().mcProfiler.endSection();
		
		
		GL11.glPopMatrix();

		GlStateManager.popMatrix();
	}
	
	/*
	 *  From WorldVertexBufferUploader.draw
	 *  
	 *  Excludes the reset buffer at the end
	 */
	public static void draw(BufferBuilder vertexBufferIn) {
		if (vertexBufferIn == null) {
			return;
		}
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

	public static String renderID(RailInfo info) {
		//TODO more attributes like railbed
		return String.format("%s%s%s%s%s%s%s%s%s", info.facing, info.type, info.direction, info.length, info.quarter, info.quarters, info.switchState, info.railBed, info.gauge);
	}
}
