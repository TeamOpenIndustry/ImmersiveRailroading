package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackItems;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderCrossing extends BuilderBase {

	public BuilderCrossing(World world, int x, int y, int z, EnumFacing rotation, int quarter) {
		super(world, x, y, z, rotation);
		
		tracks.add(new TrackGag(this, -1, 0, 0));
		tracks.add(new TrackRail(this, 0, 0, 0, EnumFacing.NORTH, TrackItems.CROSSING, 3, quarter));
		tracks.add(new TrackGag(this, 1, 0, 0));
		
		tracks.add(new TrackGag(this, -1, 0, 1));
		tracks.add(new TrackGag(this, 0, 0, 1));
		tracks.add(new TrackGag(this, 1, 0, 1));
		
		tracks.add(new TrackGag(this, -1, 0, 2));
		tracks.add(new TrackGag(this, 0, 0, 2));
		tracks.add(new TrackGag(this, 1, 0, 2));
		
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		
		data.add(new VecYawPitch(-0.5, 0, 0, 0));
		data.add(new VecYawPitch(-0.5, 0, 1, 0));
		data.add(new VecYawPitch(-0.5, 0, 2, 0));
		
		data.add(new VecYawPitch(0, 0, 0.5, -90));
		data.add(new VecYawPitch(1, 0, 0.5, -90));
		data.add(new VecYawPitch(2, 0, 0.5, -90));
		
		return data;
	}

}
