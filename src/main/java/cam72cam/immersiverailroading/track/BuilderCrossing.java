package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderCrossing extends BuilderBase {

	public BuilderCrossing(World world, int x, int y, int z, EnumFacing rotation) {
		super(world, x, y, z, rotation);
		
		tracks.add(new TrackRail(this, 0, 0, 0, EnumFacing.NORTH, TrackType.CROSSING));
		tracks.add(new TrackGag(this, 0, 0, 1));
		tracks.add(new TrackGag(this, 1, 0, 0));
		tracks.add(new TrackGag(this, 1, 0, 1));
	}

}
