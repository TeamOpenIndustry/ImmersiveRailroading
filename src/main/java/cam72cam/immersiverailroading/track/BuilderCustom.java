package cam72cam.immersiverailroading.track;

import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderCustom extends BuilderIterator {
	public BuilderCustom(RailInfo info, BlockPos pos) {
		super(info, pos);
	}
	
	private void split(List<CubicCurve2D> results, CubicCurve2D iter, double stepSize) {
		if (iter.getP1().distance(iter.getP2()) > stepSize) {
			CubicCurve2D left = new CubicCurve2D.Double();
			CubicCurve2D right = new CubicCurve2D.Double();
			iter.subdivide(left, right);
			split(results, left, stepSize);
			split(results, right, stepSize);
		} else {
			results.add(iter);
		}
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
		
		Vec3d nextPos = VecUtil.fromYaw(info.length, 45);
		
		if (info.customInfo != null) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition); 
		}
		
		List<PosStep> res = new ArrayList<PosStep>();
		
		double horizDist = nextPos.lengthVector();
		double vertDist = 0;
		float angle = info.placementInfo.rotationQuarter/4f * 90;
		if(info.placementInfo.direction == TrackDirection.RIGHT) {
			angle = -angle;
		}
		
		float angle2 = angle+90;
		
		if (info.customInfo != null) {
			angle2 = info.customInfo.rotationQuarter/4f * 90;
			if(info.customInfo.direction == TrackDirection.RIGHT) {
				angle2 = -angle2;
			}
			angle2 += info.customInfo.facing.getHorizontalAngle() - info.placementInfo.facing.getHorizontalAngle();
			
			vertDist = info.customInfo.placementPosition.y - info.placementInfo.placementPosition.y;
		}
		
		Vec3d ctrl1 = VecUtil.fromYaw(horizDist/2, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(horizDist/2, angle2));
		
		CubicCurve2D c = new CubicCurve2D.Double();
		c.setCurve(0, 0, ctrl1.x, ctrl1.z, ctrl2.x, ctrl2.z, nextPos.x, nextPos.z);
		List<CubicCurve2D> curves = new ArrayList<CubicCurve2D>();
		split(curves, c, stepSize);
		
		float slope = 0;
		if (horizDist != 0) {
			slope = (float) (vertDist / horizDist);
		}
		
		float height = slope;
		float heightAccum = 0;
		
		for (CubicCurve2D i : curves) {
			Vec3d p1 = new Vec3d(i.getP1().getX(), 0, i.getP1().getY());
			Vec3d p1ctrl = new Vec3d(i.getCtrlP1().getX(), 0, i.getCtrlP1().getY());
			Vec3d p2 = new Vec3d(i.getP2().getX(), 0, i.getP2().getY());
			float angleCurve = VecUtil.toYaw(p1ctrl.subtract(p1));
			
			res.add(new PosStep(p1.x, heightAccum, p1.z, angleCurve));
			heightAccum += p1.distanceTo(p2) * height;
		}
		System.out.println(heightAccum);
		
		cache.put(stepSize, res);
		return cache.get(stepSize);
	}
}