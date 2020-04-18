package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

public class BuilderStraight extends BuilderCubicCurve {
	public BuilderStraight(RailInfo info, World world, Vec3i pos) {
		this(info, world, pos, false);
	}

	public BuilderStraight(RailInfo info, World world, Vec3i pos, boolean endOfTrack) {
		super(info, world, pos, endOfTrack);
	}

	@Override
	public CubicCurve getCurve() {
		double length = info.settings.length - 1;
		CubicCurve curve = new CubicCurve(
				Vec3d.ZERO,
				VecUtil.fromYaw(length*0.25, info.placementInfo.yaw),
                VecUtil.fromYaw(length*0.75, info.placementInfo.yaw),
                VecUtil.fromYaw(length, info.placementInfo.yaw)
		);
		return curve;
	}
}