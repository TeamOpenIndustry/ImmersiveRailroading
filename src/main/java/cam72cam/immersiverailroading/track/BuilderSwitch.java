package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;

public class BuilderSwitch extends BuilderBase {

	private BuilderTurn turnBuilder;

	public BuilderSwitch(RailInfo info, BlockPos pos) {
		super(info, pos);
		/*
		TrackType subType = null;

		TrackRail[] myTracks = null;
		TrackRail turnTrack;
		switch (type) {
		case MEDIUM_SWITCH:
			subType = isRight ? TrackType.MEDIUM_RIGHT_SWITCH : TrackType.MEDIUM_LEFT_SWITCH;

			turnBuilder = new BuilderTurn(world, x, y, z, rotation, TrackType.MEDIUM_TURN, isRight);
			turnBuilder.setRelativeTranslate(0, 0, 1);

			turnTrack = (TrackRail) turnBuilder.tracks.get(0);
			turnTrack.setHasModel(false);
			turnTrack.setDrop(type.getItem().item);
			turnTrack.moveTo(turnBuilder.tracks.get(2));
			turnBuilder.tracks.remove(1);
			turnBuilder.tracks.remove(1);

			myTracks = new TrackRail[] { new TrackRail(this, 0, 0, 0, 0, TrackType.STRAIGHT_SMALL), new TrackRail(this, 0, 0, 1, 0, subType),
					new TrackRail(this, 0, 0, 2, 0, TrackType.STRAIGHT_SMALL), new TrackRail(this, 0, 0, 3, 0, TrackType.STRAIGHT_SMALL),
					new TrackRail(this, isRight ? -3 : 3, 0, 3, 1, TrackType.STRAIGHT_SMALL), };

			myTracks[2].setHasModel(false);

			break;

		case LARGE_SWITCH:
			subType = isRight ? TrackType.LARGE_RIGHT_SWITCH : TrackType.LARGE_LEFT_SWITCH;

			turnBuilder = new BuilderTurn(world, x, y, z, rotation, TrackType.LARGE_TURN, isRight);
			turnBuilder.setRelativeTranslate(0, 0, 1);

			turnTrack = (TrackRail) turnBuilder.tracks.get(0);
			turnTrack.setHasModel(false);
			turnTrack.setDrop(type.getItem().item);
			turnTrack.moveTo(turnBuilder.tracks.get(2));
			turnBuilder.tracks.remove(5);
			turnBuilder.tracks.remove(1);
			turnBuilder.tracks.remove(1);

			myTracks = new TrackRail[] { new TrackRail(this, 0, 0, 0, 0, TrackType.STRAIGHT_SMALL), new TrackRail(this, 0, 0, 1, 0, subType),
					new TrackRail(this, 0, 0, 2, 0, TrackType.STRAIGHT_SMALL), new TrackRail(this, 0, 0, 3, 0, TrackType.STRAIGHT_SMALL),
					new TrackRail(this, 0, 0, 4, 0, TrackType.STRAIGHT_SMALL), new TrackRail(this, 0, 0, 5, 0, TrackType.STRAIGHT_SMALL),
					new TrackRail(this, isRight ? -5 : 5, 0, 5, 1, TrackType.STRAIGHT_SMALL), };

			myTracks[2].setHasModel(false);
			myTracks[3].setHasModel(false);

			break;
		default:
			break;
		}

		for (TrackRail t : myTracks) {
			t.setDrop(null);
			tracks.add(t);
		}
		*/
	}

	@Override
	public boolean canBuild() {
		return super.canBuild() && turnBuilder.canBuild();
	}

	@Override
	public void build() {
		super.build();
		turnBuilder.build();
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		// TODO Auto-generated method stub
		return new ArrayList<VecYawPitch>();
	}
}
