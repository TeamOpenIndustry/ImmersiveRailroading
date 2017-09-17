package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class BuilderCrossing extends BuilderBase {

	public BuilderCrossing(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		this.setParentPos(new BlockPos(0, 0, 0));
		
		tracks.add(new TrackRail(this, 0, 0, 0, EnumFacing.NORTH, TrackItems.CROSSING, 3, info.quarter, info.placementPosition));
		for (int i = -1; i <=1; i ++) {
			for (int j = -1; j <=1; j ++) {
				if (i == 0 && j == 0) {
					continue;
				}
				tracks.add(new TrackGag(this, i, 0, j));
			}
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		
		data.add(new VecYawPitch(-0.5, 0, -1, 0));
		data.add(new VecYawPitch(-0.5, 0, 0, 0));
		data.add(new VecYawPitch(-0.5, 0, 1, 0));
		
		data.add(new VecYawPitch(0, 0, -0.5, -90));
		data.add(new VecYawPitch(1, 0, -0.5, -90));
		data.add(new VecYawPitch(2, 0, -0.5, -90));
		
		return data;
	}

}
