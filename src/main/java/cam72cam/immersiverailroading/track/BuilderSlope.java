package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class BuilderSlope extends BuilderBase {
	public BuilderSlope(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		float slope = (1.0F/(info.length + 1));
		TrackRail rootTrack = new TrackRail(this, 0, 0, tracks.size()/3, EnumFacing.NORTH, TrackItems.SLOPE, info.length, info.quarter, info.horizOff);
		tracks.add(new TrackGag(this, -1, 0, tracks.size()/3));
		tracks.add(rootTrack);
		tracks.add(new TrackGag(this, 1, 0, tracks.size()/3));
		for(int i = 1; i <= info.length; i ++) {
			TrackGag gag = new TrackGag(this, -1, 0, tracks.size()/3);
			TrackGag gag2 = new TrackGag(this, 0, 0, tracks.size()/3);
			TrackGag gag3 = new TrackGag(this, 1, 0, tracks.size()/3);
			
			gag.setHeight(slope * i);
			gag2.setHeight(slope * i);
			gag3.setHeight(slope * i);
			
			tracks.add(gag);
			tracks.add(gag2);
			tracks.add(gag3);
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float slope = (1.0F/(info.length + 1));

		data.add(new VecYawPitch(-0.5, 0, 0, 0, (float) -Math.toDegrees(Math.atan2(1, info.length)), info.length+1, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (int i = 0; i <= info.length; i++) {
			data.add(new VecYawPitch(-0.5, slope * i, i, 0, (float) -Math.toDegrees(Math.atan2(1, info.length)), "RAIL_BASE"));
		}
		return data;
	}
}
