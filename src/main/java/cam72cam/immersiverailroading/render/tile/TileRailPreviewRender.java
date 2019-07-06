package cam72cam.immersiverailroading.render.tile;

import cam72cam.immersiverailroading.IRItems;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class TileRailPreviewRender extends TileEntitySpecialRenderer<TileRailPreview> {
	
	@Override
	public boolean isGlobalRenderer(TileRailPreview te) {
		return true;
	}
	

	@Override
	public void render(TileRailPreview te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		RailInfo info = te.getRailRenderInfo();
		if (info == null) {
			// Still loading...
			return;
		}

		Minecraft.getMinecraft().mcProfiler.startSection("tile_rail_preview");
		GLBoolTracker blend = new GLBoolTracker(GL11.GL_BLEND, true);
		GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
		if (GLContext.getCapabilities().OpenGL14) {
			GL14.glBlendColor(1, 1, 1, 0.7f);
		}
		GL11.glPushMatrix();
		{
			// Move to specified position
			Vec3d placementPosition = info.placementInfo.placementPosition.internal;
			placementPosition = placementPosition.subtract(new Vec3d(te.getPos())).addVector(x, y, z);
			GL11.glTranslated(placementPosition.x, placementPosition.y, placementPosition.z);
			if (!te.isMulti()) {
				RailRenderUtil.render(info, true);
			}
		}
		GL11.glPopMatrix();
		GL11.glPushMatrix();
		{
			GL11.glTranslated(x, y, z);
			GL11.glTranslated(0.5, 0.5, 0.5);
			Minecraft.getMinecraft().getRenderItem().renderItem(new ItemStack(IRItems.ITEM_GOLDEN_SPIKE.internal), ItemCameraTransforms.TransformType.GROUND);
		}
		GL11.glPopMatrix();
		blend.restore();
		Minecraft.getMinecraft().mcProfiler.endSection();
	}
}
