package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.nbt.ItemTrackExchangerType;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.GLBoolTracker;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import org.lwjgl.opengl.GL11;

public class TrackExchangerModel implements ItemRender.IItemModel {
	private static OBJRender RENDERER;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if(RENDERER == null){
			try {
				RENDERER = new OBJRender(new OBJModel(new Identifier("immersiverailroading:models/item/track_exchanger/track_exchanger.obj"), -0.05f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new StandardModel().addCustom(() -> TrackExchangerModel.render(stack, world));
	}

	public static void render(ItemStack stack, World world) {
		RailInfo info = new RailInfo(world,
				new RailSettings(Gauge.from(Gauge.STANDARD), ItemTrackExchangerType.get(stack), TrackItems.STRAIGHT, 18, 0, TrackPositionType.FIXED, TrackDirection.NONE, ItemStack.EMPTY, ItemStack.EMPTY, false, false),
				new PlacementInfo(Vec3d.ZERO, TrackDirection.NONE, 0, Vec3d.ZERO),
				null,
				SwitchState.NONE,
				SwitchState.NONE,
				0);
		RailInfo lookInfo = null;
		if (MinecraftClient.getBlockMouseOver() != null) {
			RailBase railSlave = world.getBlockEntity(MinecraftClient.getBlockMouseOver(), RailBase.class);
			if (railSlave != null) {
				Rail rail = railSlave.getParentTile();
				if (rail != null) {
					lookInfo = info.withTrack(rail.info.settings.track);
				}
			}
		}

		GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, true);
		GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);

		GL11.glPushMatrix();
		RENDERER.bindTexture();
		RENDERER.draw();
		RENDERER.restoreTexture();

		GL11.glScaled(0.01, 0.01, 0.01);
		GL11.glRotated(90, 1, 0, 0);

		GL11.glTranslated(-15.15, 0.75, -8.75);
		RailBaseRender.draw(info);
		RailBuilderRender.renderRailBuilder(info);

		if (lookInfo != null) {
			GL11.glTranslated(-22.05, 0, 0);
			RailBaseRender.draw(lookInfo);
			RailBuilderRender.renderRailBuilder(lookInfo);
		}

		GL11.glPopMatrix();

		tex.restore();
		cull.restore();
	}

	@Override
	public void applyTransform(ItemRender.ItemRenderType type) {
		switch (type) {
			case THIRD_PERSON_LEFT_HAND:
				GL11.glTranslated(1.15, 0.5, 0.5);
				GL11.glScaled(1.5, 1.5, 1.5);
				break;
			case THIRD_PERSON_RIGHT_HAND:
				GL11.glTranslated(0.5, 0.5, 0.5);
				GL11.glScaled(1.5, 1.5, 1.5);
				break;
			case FIRST_PERSON_LEFT_HAND:
				GL11.glRotated(20, 0, 1, 0);
				GL11.glTranslated(0.7, 0.8, 0.7);
				GL11.glScaled(1.5, 1.5, 1.5);
				break;
			case FIRST_PERSON_RIGHT_HAND:
				GL11.glRotated(-20, 0, 1, 0);
				GL11.glTranslated(0.8, 0.8, 0.5);
				GL11.glScaled(1.5, 1.5, 1.5);
				break;
			case ENTITY:
				GL11.glTranslated(1, 0.4, 0.5);
				GL11.glRotated(-90, 1, 0, 0);
				GL11.glScaled(2, 2, 2);
				break;
			case FRAME:
				GL11.glRotated(180, 1, 0, 0);
				GL11.glRotated(-135, 0, 0, 1);
				GL11.glTranslated(0.55, 0.75, -0.55);
				GL11.glScaled(2.5, 2.5, 2.5);
				break;
			case GUI:
				GL11.glTranslated(0.95, 0.5, 0);
				GL11.glScaled(2, 2, 2);
				break;
		}
	}
}
