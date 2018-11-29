package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderSlope extends BuilderStraight {
	public BuilderSlope(RailInfo info, BlockPos pos) {
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
