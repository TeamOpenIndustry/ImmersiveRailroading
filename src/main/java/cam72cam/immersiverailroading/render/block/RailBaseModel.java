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
import cam72cam.mod.render.cutter.BlockCutHelper;
import cam72cam.mod.render.cutter.Plane;
import util.Matrix4;

public class RailBaseModel {
	public static StandardModel getModel(TileRailBase te) {
		ItemStack bed = te.getRenderRailBed();
		if (bed == null) {
            // wait for tile to be initialized
			return null;
		}

		float bedHeight = te.getBedHeight();
        Plane bedFace = te.getBedFace();
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
                if (info.settings.type.isTable()) {
					ItemStack held = MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY);
                	if (held.is(IRItems.ITEM_TRACK_BLUEPRINT) || held.is(IRItems.ITEM_GOLDEN_SPIKE)) {
						info = info.with(b -> b.itemHeld = true);
					}

				}

				RailRender.get(info).renderRailModel(state.translate(info.placementInfo.placementPosition));
			});
		}

		Matrix4 bedblockMatrix4;
		if(bedHeight > 0) bedblockMatrix4 = new Matrix4().scale(1, bedHeight, 1);
		else if(bedHeight == 0) bedblockMatrix4 = new Matrix4().scale(1, 1e-4, 1);
		else bedblockMatrix4 = new Matrix4().translate(0, -bedHeight, 0).scale(1, 1 + bedHeight, 1);

		if (augment != null) {
			model.addColorBlock(augment.color(), bedFace == null ? bedblockMatrix4 : new Matrix4(), bedFace);
			return model;
		}

		boolean renderSnow = false;
		if(bedHeight > 0) renderSnow = snow > Math.floor(bedHeight * 8f);
		else renderSnow = snow > 7;

		if (renderSnow) {

			float snowHeight = snow / 8f;
			if(bedFace != null) {
				boolean isUp = bedHeight >= 0;
				if(!isUp) {
					model.addSnow(8, new Matrix4(), bedFace);
					return model;
				}

				float planeMinHeight = BlockCutHelper.getCutPlaneMinHeight(bedFace);
				float planeMaxHeight = BlockCutHelper.getCutPlaneMaxHeight(bedFace);

				if (planeMaxHeight <= snowHeight) {
					model.addSnow(snow, new Matrix4(), BlockCutHelper.createBottomSidePlane(bedFace));
				} else if (planeMinHeight >= snowHeight) {
					model.addSnow(8, new Matrix4(), bedFace);
				} else {
					model.addSnow(snow, new Matrix4(), BlockCutHelper.createBottomSidePlane(bedFace));
					model.addSnow(8, new Matrix4(), bedFace);
				}
			} else {
				model.addSnow(snow, new Matrix4(), null);
			}

			return model;
		} else if (!bed.isEmpty()) {
			model.addItemBlock(bed, bedFace == null ? bedblockMatrix4 : new Matrix4(), bedFace);
			return model;
		}

		return model;
	}
}
