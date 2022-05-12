package cam72cam.immersiverailroading.render.item;

import cam72cam.immersiverailroading.items.ItemTrackExchanger;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.render.rail.RailBuilderRender;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.ItemRender;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

public class TrackExchangerModel implements ItemRender.IItemModel {
	private static OBJModel MODEL;

	@Override
	public StandardModel getModel(World world, ItemStack stack) {
		if(MODEL == null){
			try {
				MODEL = new OBJModel(new Identifier("immersiverailroading:models/item/track_exchanger/track_exchanger.obj"), -0.05f, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new StandardModel().addCustom((state, pt) -> TrackExchangerModel.render(stack, world, state));
	}

	public static void render(ItemStack stack, World world, RenderState state) {
		ItemTrackExchanger.Data data = new ItemTrackExchanger.Data(stack);
		RailInfo info = new RailInfo(
                new RailSettings(data.gauge, data.track, TrackItems.STRAIGHT, 18, 0, 1, TrackPositionType.FIXED, TrackSmoothing.BOTH, TrackDirection.NONE, data.railBed, ItemStack.EMPTY, false, false),
				new PlacementInfo(Vec3d.ZERO, TrackDirection.NONE, 0, Vec3d.ZERO),
				null,
				SwitchState.NONE,
				SwitchState.NONE,
				0);
		RailInfo lookInfo = null;
		if (MinecraftClient.getBlockMouseOver() != null) {
			TileRailBase railSlave = world.getBlockEntity(MinecraftClient.getBlockMouseOver(), TileRailBase.class);
			if (railSlave != null) {
				TileRail rail = railSlave.getParentTile();
				if (rail != null) {
					lookInfo = info.withTrack(rail.info.settings.track).withRailBed(rail.info.settings.railBed).withGauge(rail.info.settings.gauge);
				}
			}
		}

		try (OBJRender.Binding vbo = MODEL.binder().bind(state)) {
			vbo.draw();
		}

		state.lighting(false);
		state.scale(0.01, 0.01, 0.01);
		state.rotate(90, 1, 0, 0);

		state.translate(-15.15, 0.75, -8.75);
		RailBaseRender.draw(info, world, state);
		RailBuilderRender.renderRailBuilder(info, world, state);

		if (lookInfo != null) {
			state.translate(-22.05, 0, 0);
			RailBaseRender.draw(lookInfo, world, state);
			RailBuilderRender.renderRailBuilder(lookInfo, world, state);
		}
	}

	@Override
	public void applyTransform(ItemStack stack, ItemRender.ItemRenderType type, RenderState state) {
		switch (type) {
			case THIRD_PERSON_LEFT_HAND:
				state.translate(1.15, 0.5, 0.5);
				state.scale(1.5, 1.5, 1.5);
				break;
			case THIRD_PERSON_RIGHT_HAND:
				state.translate(0.5, 0.5, 0.5);
				state.scale(1.5, 1.5, 1.5);
				break;
			case FIRST_PERSON_LEFT_HAND:
				state.rotate(20, 0, 1, 0);
				state.translate(0.7, 0.8, 0.7);
				state.scale(1.5, 1.5, 1.5);
				break;
			case FIRST_PERSON_RIGHT_HAND:
				state.rotate(-20, 0, 1, 0);
				state.translate(0.8, 0.8, 0.5);
				state.scale(1.5, 1.5, 1.5);
				break;
			case ENTITY:
				state.translate(1, 0.4, 0.5);
				state.rotate(-90, 1, 0, 0);
				state.scale(2, 2, 2);
				break;
			case FRAME:
				state.rotate(180, 1, 0, 0);
				state.rotate(-135, 0, 0, 1);
				state.translate(0.55, 0.75, -0.55);
				state.scale(2.5, 2.5, 2.5);
				break;
			case GUI:
				state.translate(0.95, 0.5, 0);
				state.scale(2, 2, 2);
				break;
		}
	}
}
