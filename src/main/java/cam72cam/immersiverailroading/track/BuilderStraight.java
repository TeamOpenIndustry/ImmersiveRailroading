package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import util.Matrix4;

import java.util.List;

public class BuilderStraight extends BuilderCubicCurve {
	protected float angle;

	public BuilderStraight(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public BuilderStraight(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos, endOfTrack);
	}

	@Override
	public List<PosStep> getPath(double stepSize) {
		double length = info.settings.length;
		float angle = info.placementInfo.rotationQuarter/4f * 90;
		if(info.placementInfo.direction == TrackDirection.RIGHT) {
			angle = -angle;
		}

		CubicCurve curve = new CubicCurve(
				Vec3d.ZERO,
				new Vec3d(0.25, 0,0),
				new Vec3d(0.75, 0, 0),
				new Vec3d(1, 0,0)
		);
		curve = curve.apply(new Matrix4().scale(length-1, length-1, length-1).rotate(Math.toRadians(180 + 90-angle), 0, 1, 0));
		return getPath(curve, stepSize);
	}
}