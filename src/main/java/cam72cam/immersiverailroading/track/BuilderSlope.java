package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class BuilderSlope extends BuilderStraight {
	public BuilderSlope(RailInfo info, Vec3i pos) {
		super(info, pos);
	}
	
	public List<PosStep> getPath(double stepSize) {
		List<PosStep> res = new ArrayList<PosStep>();
		
		float slope = (1.0F/(info.settings.length));
		float startHeight = slope/2;

		for (PosStep cur : super.getPath(stepSize)) {
			res.add(new PosStep(cur.x, cur.y + startHeight + slope * cur.distanceTo(Vec3d.ZERO), cur.z, cur.yaw, (float)-Math.toDegrees(Math.atan(slope))));
		}
		
		return res;
	}
}
