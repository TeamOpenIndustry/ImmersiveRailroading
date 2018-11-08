package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderStraight extends BuilderIterator {
	protected float angle;
	public int mainX;
	public int mainZ;
	
	public BuilderStraight(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public BuilderStraight(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos, endOfTrack);
	}

	@Override
	public List<PosStep> getPath(double stepSize) {
		List<PosStep> res = new ArrayList<PosStep>();
		
		float angle = info.placementInfo.rotationQuarter/4f * 90;
		if(info.placementInfo.direction == TrackDirection.RIGHT) {
			angle = -angle;
		}
		
		for (float dist = 0; dist < info.length; dist += stepSize) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			res.add(new PosStep(gagPos, angle));
		}
		return res;
	}
}