package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;

public class BuilderSlope extends BuilderStraight {
	public BuilderSlope(RailInfo info, BlockPos pos) {
		super(info, pos);
	}
	
	public List<PosStep> getPath(double stepSize) {
		List<PosStep> res = new ArrayList<PosStep>();
		
		float slope = (1.0F/(info.settings.length));
		
		float height = slope/2;
		
		for (PosStep cur : super.getPath(stepSize)) {
			res.add(new PosStep(cur.x, cur.y + height, cur.z, cur.yaw));
			height += slope * stepSize;
		}
		
		return res;
	}
}
