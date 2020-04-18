package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.render.ExpireableList;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.render.rail.RailRenderUtil;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.render.*;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class TrackBlueprintItemModel implements ItemRender.IItemModel {
	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		return new StandardModel().addCustom(() -> TrackBlueprintItemModel.render(stack, world));
	}
	public static void render(ItemStack stack, World world) {
		RailInfo info = new RailInfo(stack, new PlacementInfo(stack, 1, new Vec3d(0.5, 0.5, 0.5)), null);
		info = info.withLength(10);

		GL11.glPushMatrix();
		{
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			GLBoolTracker lighting = new GLBoolTracker(GL11.GL_LIGHTING, false);

			if (info.settings.type == TrackItems.TURN || info.settings.type == TrackItems.SWITCH) {
				GL11.glTranslated(0, 0, -0.1 * (info.settings.degrees / 90 * 4));
			}

			GL11.glTranslated(0.5, 0, 0.5);

			GL11.glRotated(-90, 1, 0, 0);


			double scale = 0.95 / info.settings.length;
			if (info.settings.type == TrackItems.CROSSING) {
				scale = 0.95 / 3;
			}
			if (info.settings.type == TrackItems.TURNTABLE) {
				scale *= 0.25;
			}
			GL11.glScaled(-scale, -scale * 2, scale);

			GL11.glTranslated(0.5, 0, 0.5);

			GL11.glPushMatrix();
			{
				GL11.glTranslated(-0.5, 0, -0.5);
				RailBaseRender.draw(info, world);
			}
			GL11.glPopMatrix();
			RailBuilderRender.renderRailBuilder(info, world);

			lighting.restore();
			cull.restore();
		}
		GL11.glPopMatrix();
	}

	private static ExpireableList<String, RailInfo> infoCache = new ExpireableList<>();
	public static void renderMouseover(Player player, ItemStack stack, Vec3i pos, Vec3d vec, float partialTicks) {
		Vec3d hit = vec.subtract(pos);
		World world = player.getWorld();

		pos = pos.up();

		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			if (!BlockUtil.isIRRail(world, pos.down()) || world.getBlockEntity(pos.down(), TileRailBase.class).getRailHeight() < 0.5) {
				pos = pos.down();
			}
		}

		RailInfo info = new RailInfo(stack, new PlacementInfo(stack, player.getRotationYawHead(), hit.subtract(0, hit.y, 0)), null);
		String key = info.uniqueID + info.placementInfo.placementPosition;
		RailInfo cached = infoCache.get(key);
		if (cached != null) {
			info = cached;
		} else {
			infoCache.put(key, info);
		}

		GL11.glPushMatrix();
		{
			GLTransparencyHelper transparency = new GLTransparencyHelper(1,1,1, 0.5f);

			Vec3d cameraPos = GlobalRender.getCameraPos(partialTicks);
			Vec3d offPos = info.placementInfo.placementPosition.add(pos).subtract(cameraPos);
			GL11.glTranslated(offPos.x, offPos.y, offPos.z);

			RailRenderUtil.render(info, world, pos, true);

			transparency.restore();
		}
		GL11.glPopMatrix();

	}
}
