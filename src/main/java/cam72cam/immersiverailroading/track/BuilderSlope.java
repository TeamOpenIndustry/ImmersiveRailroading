package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderSlope extends BuilderBase {

	private int length;

	@SuppressWarnings("incomplete-switch")
	public BuilderSlope(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		
		length = 0;
		switch(type) {
		case SLOPE_MEDIUM:
			length = 23;
			break;
		case SLOPE_LARGE:
			length = 35;
			break;
		}
		
		float slope = (1.0F/(length + 1));
		TrackRail rootTrack = new TrackRail(this, 0, 0, tracks.size()/2, EnumFacing.NORTH, type);
		rootTrack.setSlope(1, length);
		tracks.add(rootTrack);
		tracks.add(new TrackGag(this, 1, 0, tracks.size()/2));
		for(int i = 1; i <= length; i ++) {
			TrackGag gag = new TrackGag(this, 0, 0, tracks.size()/2);
			TrackGag gag2 = new TrackGag(this, 1, 0, tracks.size()/2);
			
			gag.setHeight(slope * i);
			gag2.setHeight(slope * i);
			
			tracks.add(gag);
			tracks.add(gag2);
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float slope = (1.0F/(length + 1));
		
		for (int i = 0; i <= length; i++) {
			data.add(new VecYawPitch(0, slope * i, i, 0, (float) -Math.toDegrees(Math.atan2(1, length))));
		}
		return data;
	}
}
