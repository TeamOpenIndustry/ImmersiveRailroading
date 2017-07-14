package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderCrossing extends BuilderBase {

	public BuilderCrossing(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		/*
		switch(type) {
		case SMALL_ROAD_CROSSING:
			tracks.add(new TrackRail(this, 0, 0, 0, 0, type));
			break;
		case TWO_WAYS_CROSSING:
			tracks.add(new TrackRail(this, 0, 0, 0, 0, TrackType.STRAIGHT_SMALL));
			tracks.add(new TrackRail(this, 0, 0, 1, 0, type));
			tracks.add(new TrackRail(this, 0, 0, 2, 0, TrackType.STRAIGHT_SMALL));
			tracks.add(new TrackRail(this, 0, 0, 1, 1, TrackType.STRAIGHT_SMALL));
			tracks.add(new TrackRail(this, 2, 0, 1, 1, TrackType.STRAIGHT_SMALL));
		default:
			break;
		}*/
	}

}
