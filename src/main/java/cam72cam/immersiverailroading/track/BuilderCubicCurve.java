package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderCubicCurve extends BuilderIterator {
	public BuilderCubicCurve(RailInfo info, BlockPos pos) {
		super(info, pos);
	}

	private HashMap<Double, List<PosStep>> cache; 

	@Override
	public List<PosStep> getPath(double stepSize) {
		Vec3d nextPos = VecUtil.fromYaw(info.settings.length, 45);

		if (info.customInfo != null && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition);
		}

		double horizDist = nextPos.lengthVector();
		float angle = info.placementInfo.rotationQuarter / 4f * 90;
		if (info.placementInfo.direction == TrackDirection.RIGHT) {
			angle = -angle;
		}

		float angle2 = angle - 90;

		if (info.customInfo != null) {
			angle2 = info.customInfo.rotationQuarter / 4f * 90;
			if (info.customInfo.direction == TrackDirection.RIGHT) {
				angle2 = -angle2;
			}
			angle2 -= (info.placementInfo.facing.getHorizontalAngle() - info.customInfo.facing.getHorizontalAngle());
			nextPos = VecUtil.rotateYaw(nextPos, 180 - (info.placementInfo.facing.getHorizontalAngle() - 90));
		}

		Vec3d ctrl1 = VecUtil.fromYaw(horizDist / 2, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(horizDist / 2, angle2));
		return getPath(new CubicCurve(Vec3d.ZERO, ctrl1, ctrl2, nextPos), stepSize);
	}

    public List<PosStep> getPath(CubicCurve curve, double stepSize) {
		if (cache == null) {
			cache = new HashMap<Double, List<PosStep>>();
		}

		if (cache.containsKey(stepSize)) {
			return cache.get(stepSize);
		}

		List<PosStep> res = new ArrayList<PosStep>();

		List<Vec3d> points = curve.toList(stepSize);
		for(int i = 0; i < points.size(); i++) {
			Vec3d p = points.get(i);
			float angleCurve;
			if (i == points.size()-1) {
				angleCurve = curve.angleStop();
			} else if (i == 0) {
				angleCurve = curve.angleStart();
			} else {
				angleCurve = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
			}
			res.add(new PosStep(p, angleCurve));
		}
		cache.put(stepSize, res);
		return cache.get(stepSize);
	}
}