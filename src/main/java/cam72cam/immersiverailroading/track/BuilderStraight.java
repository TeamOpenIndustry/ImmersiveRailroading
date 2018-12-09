package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderStraight extends BuilderCubicCurve {
	public BuilderStraight(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public BuilderStraight(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos, endOfTrack);
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