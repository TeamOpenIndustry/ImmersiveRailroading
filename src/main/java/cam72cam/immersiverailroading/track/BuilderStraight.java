package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderStraight extends BuilderBase {
	private int length;

	public BuilderStraight(World world, int x, int y, int z, EnumFacing rotation, int length) {
		super(world, x, y, z, rotation);
		
		this.length = length;
		
		int long_tracks = length / 16;
		int med_tracks = (length % 16) / 8;
		int short_tracks = ((length % 16) % 8) / 2;
		
		for (;long_tracks > 0; long_tracks --) {
			addTracks(TrackType.STRAIGHT_LONG, 16);
		}
		for (;med_tracks > 0; med_tracks --) {
			addTracks(TrackType.STRAIGHT_MEDIUM, 8);
		}
		for (;short_tracks > 0; short_tracks --) {
			addTracks(TrackType.STRAIGHT_SMALL, 2);
		}
	}
	
	private void addTracks(TrackType type, int length) {
		tracks.add(new TrackRail(this, 0, 0, tracks.size()/2, EnumFacing.NORTH, type));
		tracks.add(new TrackGag(this, 1, 0, tracks.size()/2));
		for(int i = 1; i < length; i ++) {
			tracks.add(new TrackGag(this, 0, 0, tracks.size()/2));
			tracks.add(new TrackGag(this, 1, 0, tracks.size()/2));
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		for (int i = 0; i < length; i++) {
			data.add(new VecYawPitch(0, 0, i, 0));
		}
		return data;
	}
}