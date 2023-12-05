package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.render.rail.RailRender;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import util.Matrix4;

public class RailBaseModel {
	public static StandardModel getModel(TileRailBase te) {
		ItemStack bed = te.getRenderRailBed();
		if (bed == null) {
            // wait for tile to be initialized
			return null;
		}

		float height = te.getBedHeight();
		float tileHeight = height;
		int snow = te.getSnowLayers();
		Augment augment = te.getAugment();
		double gauged = te.getRenderGauge();
		Gauge gauge = Gauge.from(gauged);

		StandardModel model = new StandardModel();
		if (te instanceof TileRail && ((TileRail) te).info != null) {
			model.addCustom((state, pt) -> {
				RailInfo info = ((TileRail) te).info;
                if (info.settings.type == TrackItems.SWITCH) {
                    //TODO render switch and don't render turn
                    info = info.withSettings(b -> b.type = TrackItems.STRAIGHT);
                }
                if (info.settings.type == TrackItems.TURNTABLE) {
					ItemStack held = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
                	if (held.is(IRItems.ITEM_TRACK_BLUEPRINT) || held.is(IRItems.ITEM_GOLDEN_SPIKE)) {
						info = info.with(b -> b.itemHeld = true);
					}
				}

				RailRender.get(info).renderRailModel(state.translate(info.placementInfo.placementPosition));
			});
		}

		if (augment != null) {
			height = height + 0.1f * (float)gauge.scale() * 1.25f;

			model.addColorBlock(augment.color(), new Matrix4().scale(1, height, 1));
			return model;
		}

		height = height + 0.1f * (float)gauge.scale();

		if (snow != 0) {
			model.addSnow(snow + (int)(height * 8), new Matrix4());
			return model;
		} else if (!bed.isEmpty() && tileHeight != 0.000001f) {
			model.addItemBlock(bed, new Matrix4().scale(1, height, 1));
			return model;
		}

		return model;
	}
}
