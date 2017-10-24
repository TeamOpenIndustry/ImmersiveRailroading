package cam72cam.immersiverailroading.render.rail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;

public class TileRailBaseRender<F extends TileRailBase> extends TileEntitySpecialRenderer<F> {
	private BlockRendererDispatcher blockRenderer;
	private Map<String, Integer> displayLists = new HashMap<String, Integer>();
	
	private int renderModel(IBlockState state, IBakedModel model, int light) {
		System.out.println("NEW LIST");
		BufferBuilder buffer = new BufferBuilder(2048);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		buffer.color(255, 255, 255, 255);

		List<BakedQuad> quads = model.getQuads(state, null, 0);
		for (BakedQuad quad : quads) {
			buffer.addVertexData(quad.getVertexData());
            buffer.putBrightness4(light, light, light, light);
			buffer.putPosition(0,0,0);
		}
		int oldLight = light >> 20;
		int dec = 5;
		if (oldLight < dec) {
			dec = oldLight;
		}
		light -= dec << 20;
		for (EnumFacing facing : EnumFacing.VALUES) {
			quads = model.getQuads(state, facing, 0);
			for (BakedQuad quad : quads) {
				//buffer.color(1, 1, 1, 1);
				buffer.addVertexData(quad.getVertexData()); 
	            buffer.putBrightness4(light, light, light, light);
				buffer.putPosition(0,0,0);
			}
		}
		buffer.finishDrawing();
		
		int displayList = GL11.glGenLists(1);
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		RailRenderUtil.draw(buffer);
		GL11.glEndList();
		return displayList;
	}
	
	@Override
	public void render(TileRailBase te, double x, double y, double z, float partialTicks, int destroyStage, float partial) {
		if (blockRenderer == null) {
			blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		}
    	Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		
		int light = te.getWorld().getBlockState(te.getPos()).getPackedLightmapCoords(te.getWorld(), te.getPos());
		
		GLBoolTracker tracker = new GLBoolTracker(GL11.GL_LIGHTING, false);
		
		int snowLevel = te.getSnowLayers();
		if (snowLevel != 0) {
			String id = snowLevel + ":" + light;
			if (!displayLists.containsKey(id)) {
				IBlockState state = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, snowLevel);
				IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);
				displayLists.put(id, renderModel(state, model, light));
			}
			GL11.glPushMatrix();
				GL11.glTranslated(x, y, z);
				GL11.glCallList(displayLists.get(id));
			GL11.glPopMatrix();
		} else {
			TileRail parent = te.getParentTile();
			if (parent != null) {
				RailInfo info = parent.getRailRenderInfo();
				if (info == null) {
					return;
				}
				if (info.railBed.getItem() == Items.AIR) {
					return;
				}

				String str = info.railBed.toString();
				String id = str + ":" + te.getHeight() + ":" + light;
				
				if (!displayLists.containsKey(id)) {
					IBlockState state = BlockUtil.itemToBlockState(info.railBed);
					IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);
					model = new ScaledModel(model, te.getHeight());
					displayLists.put(id, renderModel(state, model, light));
				}
				GL11.glPushMatrix();
					GL11.glTranslated(x, y, z);
					GL11.glCallList(displayLists.get(id));
				GL11.glPopMatrix();
			}
		}
		tracker.restore();
	}
}
