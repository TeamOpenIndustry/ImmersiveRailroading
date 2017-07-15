package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderSlope extends BuilderBase {

	@SuppressWarnings("incomplete-switch")
	public BuilderSlope(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		
		int gags = 0;
		switch(type) {
		case SLOPE_MEDIUM:
			gags = 23;
			break;
		case SLOPE_LARGE:
			gags = 35;
			break;
		}
		
		float slope = (1.0F/(gags + 1));
		tracks.add(new TrackRail(this, 0, 0, tracks.size()/2, EnumFacing.NORTH, type));
		tracks.add(new TrackGag(this, 1, 0, tracks.size()/2));
		for(int i = 1; i <= gags; i ++) {
			TrackGag gag = new TrackGag(this, 0, 0, tracks.size()/2);
			TrackGag gag2 = new TrackGag(this, 1, 0, tracks.size()/2);
			
			gag.setHeight(slope * (i + 1));
			gag2.setHeight(slope * (i + 1));
			
			tracks.add(gag);
			tracks.add(gag2);
		}
	}
}
