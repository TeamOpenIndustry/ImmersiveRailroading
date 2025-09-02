package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

public class BuilderSlope extends BuilderStraight {
	public BuilderSlope(RailInfo info, World world, Vec3i pos) {
		super(info, world, pos);
	}

	@Override
	public CubicCurve getCurve() {
		CubicCurve curve = super.getCurve();
		double delta = 1;
		if (info.customInfo != null && info.customInfo.placementPosition != null && info.customInfo != info.placementInfo) {
			delta = -info.placementInfo.placementPosition.subtract(info.customInfo.placementPosition).y;
		}
		curve = new CubicCurve(curve.p1, curve.ctrl1, curve.ctrl2.add(0, delta, 0), curve.p2.add(0, delta, 0));
		return curve.linearize(info.settings.smoothing);
	}
}
