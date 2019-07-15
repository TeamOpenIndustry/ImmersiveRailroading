package cam72cam.immersiverailroading.render.block;

import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;

public class RailBaseModel {
	public static StandardModel getModel(RailBase te) {
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

		if (augment != null) {
			height = height + 0.1f * (float)gauge.scale() * 1.25f;

			model.addColorBlock(augment.color(), Vec3d.ZERO, new Vec3d(1, height, 1));
			return model;
		}

		height = height + 0.1f * (float)gauge.scale();

		if (snow != 0) {
			model.addSnow(snow + (int)(height * 8), Vec3d.ZERO);
			return model;
		} else if (!bed.isEmpty() && tileHeight != 0.000001f) {
			model.addItem(bed, Vec3d.ZERO, new Vec3d(1, height, 1));
			return model;
		}

		return null;
	}
}
