package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderSlope extends BuilderBase {

	public BuilderSlope(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		/*
		int gags = 0;
		switch(type) {
		case SLOPE_WOOD:
		case SLOPE_GRAVEL:
		case SLOPE_BALLAST:
			gags = 5;
			break;
		case LARGE_SLOPE_WOOD:
		case LARGE_SLOPE_GRAVEL:
		case LARGE_SLOPE_BALLAST:
			gags = 11;
			break;
		case VERY_LARGE_SLOPE_WOOD:
		case VERY_LARGE_SLOPE_GRAVEL:
		case VERY_LARGE_SLOPE_BALLAST:
			gags = 17;
			break;
		default:
			break;
		}
		
		float slope = (1.0F/(gags + 1));
		System.out.println(slope);
		TrackRail rail = new TrackRail(this, 0, 0, tracks.size(), 0, type);
		tracks.add(rail);
		for(int i = 1; i <= gags; i ++) {
			TrackGag gag = new TrackGag(this, 0, 0, tracks.size(), 0, type);
			gag.setHeight(slope * (i + 1));
			tracks.add(gag);
		}
		*/
	}
}
