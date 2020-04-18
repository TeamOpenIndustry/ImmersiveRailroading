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
		curve = new CubicCurve(curve.p1, curve.ctrl1, curve.ctrl2.add(0, 1, 0), curve.p2.add(0, 1, 0));
		return curve.linearize(info.settings.smoothing);
	}
}
