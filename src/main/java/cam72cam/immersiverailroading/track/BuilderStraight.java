package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.TrackItems;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderStraight extends BuilderBase {
	private int length;

	public BuilderStraight(World world, int x, int y, int z, EnumFacing rotation, int length, int quarter) {
		super(world, x, y, z, rotation);
		
		this.length = length;
		
		int long_tracks = length / 16;
		int med_tracks = (length % 16) / 8;
		int short_tracks = ((length % 16) % 8) / 2;
		
		for (;long_tracks > 0; long_tracks --) {
			addTracks(16, quarter);
		}
		for (;med_tracks > 0; med_tracks --) {
			addTracks(8, quarter);
		}
		for (;short_tracks > 0; short_tracks --) {
			addTracks(2, quarter);
		}
	}
	
	private void addTracks(int length, int quarter) {
		tracks.add(new TrackGag(this, -1, 0, tracks.size()/3));
		tracks.add(new TrackRail(this, 0, 0, tracks.size()/3, EnumFacing.NORTH, TrackItems.STRAIGHT, length, quarter));
		tracks.add(new TrackGag(this, 1, 0, tracks.size()/3));
		for(int i = 1; i < length; i ++) {
			tracks.add(new TrackGag(this, -1, 0, tracks.size()/3));
			tracks.add(new TrackGag(this, 0, 0, tracks.size()/3));
			tracks.add(new TrackGag(this, 1, 0, tracks.size()/3));
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		data.add(new VecYawPitch(-0.5, 0, 0, 0, 0, length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (int i = 0; i < length; i++) {
			data.add(new VecYawPitch(-0.5, 0, i, 0, "RAIL_BASE"));
		}
		return data;
	}
}