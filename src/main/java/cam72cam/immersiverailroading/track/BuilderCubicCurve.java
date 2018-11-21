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
	
	private static Vec3d cubicAt(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2, double t) {
		Vec3d pt = Vec3d.ZERO;
		pt = pt.add(p1.		scale(1 * Math.pow(1-t, 3) * Math.pow(t, 0)));
		pt = pt.add(ctrl1.	scale(3 * Math.pow(1-t, 2) * Math.pow(t, 1)));
		pt = pt.add(ctrl2.	scale(3 * Math.pow(1-t, 1) * Math.pow(t, 2)));
		pt = pt.add(p2.		scale(1 * Math.pow(1-t, 0) * Math.pow(t, 3)));
		return pt;
	}
	
	private static List<Vec3d> cubicSplit(Vec3d p1, Vec3d ctrl1, Vec3d ctrl2, Vec3d p2, double stepSize) {
		List<Vec3d> res = new ArrayList<Vec3d>();
		res.add(p1);
		double precision = 5;
		
		double t = 0;
		while (t <= 1) {
			for (double i = 1; i < precision; i++) {
				Vec3d prev = res.get(res.size()-1);
				
				double delta = (Math.pow(10, -i)); 
				
				for (;t < 1 + delta; t+=delta) {
					Vec3d pos = cubicAt(p1, ctrl1, ctrl2, p2, t);
					if (pos.distanceTo(prev) > stepSize) {
						// We passed it, just barely
						t -= delta;
						break;
					}
				}
			}
			if (t <= 1) {
				res.add(cubicAt(p1, ctrl1, ctrl2, p2, t));
			}
		}
		return res;
	}
	
	private HashMap<Double, List<PosStep>> cache; 

	@Override
	public List<PosStep> getPath(double stepSize) {
		if (cache == null) {
			cache = new HashMap<Double, List<PosStep>>();
		}
		
		if (cache.containsKey(stepSize)) {
			return cache.get(stepSize);
		}
		
		Vec3d nextPos = VecUtil.fromYaw(info.settings.length, 45);

		if (info.customInfo != null && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition); 
		}
		
		List<PosStep> res = new ArrayList<PosStep>();
		
		double horizDist = nextPos.lengthVector();
		float angle = info.placementInfo.rotationQuarter/4f * 90;
		if(info.placementInfo.direction == TrackDirection.RIGHT) {
			angle = -angle;
		}
		
		float angle2 = angle-90;
		
		if (info.customInfo != null) {
			angle2 = info.customInfo.rotationQuarter/4f * 90;
			if(info.customInfo.direction == TrackDirection.RIGHT) {
				angle2 = -angle2;
			}
			angle2 -= (info.placementInfo.facing.getHorizontalAngle() - info.customInfo.facing.getHorizontalAngle());
			nextPos = VecUtil.rotateYaw(nextPos, 180 - (info.placementInfo.facing.getHorizontalAngle() - 90));
		}
		
		Vec3d ctrl1 = VecUtil.fromYaw(horizDist/2, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(horizDist/2, angle2));

		List<Vec3d> points = cubicSplit(Vec3d.ZERO, ctrl1, ctrl2, nextPos, stepSize);
		for(int i = 0; i < points.size(); i++) {
			Vec3d p1 = points.get(i);
			float angleCurve;
			if (i == points.size()-1) {
				angleCurve = angle2+180;
			} else if (i == 0) {
				angleCurve = angle;
			} else {
				angleCurve = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
			}
			res.add(new PosStep(p1, angleCurve));
		}
		
		cache.put(stepSize, res);
		return cache.get(stepSize);
	}
}