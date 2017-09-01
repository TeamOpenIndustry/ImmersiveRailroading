package cam72cam.immersiverailroading.track;

import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;

public class BuilderParallel extends BuilderBase {

	public BuilderParallel(RailInfo info, BlockPos pos) {
		super(info, pos);
		/*
		TrackType subType = isRight ? TrackType.MEDIUM_RIGHT_PARALLEL_SWITCH : TrackType.MEDIUM_LEFT_PARALLEL_SWITCH;
		TrackType subTypeTurn = isRight ? TrackType.MEDIUM_RIGHT_TURN : TrackType.MEDIUM_LEFT_TURN;
		TrackType subTypeOther = !isRight ? TrackType.MEDIUM_RIGHT_TURN : TrackType.MEDIUM_LEFT_TURN;

		int xMult = isRight ? -1 : 1;
		int zMult = 1;

		for (int straight = 0; straight < 10; straight++) {
			TrackRail track = new TrackRail(this, 0, 0, straight, 0, straight == 1 ? subType : TrackType.STRAIGHT_SMALL);
			if (straight == 2 || straight == 3) {
				track.setHasModel(false);
			}
			tracks.add(track);
		}

		int[] xArray = { 1, 1, 1, 1 };
		int[] zArray = { 3, 2, 4, 5 };

		TrackRail turnTrack = new TrackRail(this, xArray[0] * xMult, 0, zArray[0] * zMult, 0, subTypeTurn);
		turnTrack.setDrop(type.getItem().item);
		turnTrack.setHasModel(false);
		// TODO This is a guess
		turnTrack.setRotationCenter(xMult * 8, 0, zMult * 1, 8.5f);

		tracks.add(turnTrack);
		for (int i = 1; i < xArray.length; i++) {
			tracks.add(new TrackGag(this, xArray[i] * xMult, 0, zArray[i] * zMult, 0, subTypeTurn));
		}

		int[] xArray2 = { 2, 2, 2, 2, 2, 3, 3, 3, 3 };
		int[] zArray2 = { 4, 5, 6, 7, 8, 6, 7, 8, 9 };

		TrackRail turnTrack2 = new TrackRail(this, xArray2[0] * xMult, 0, zArray2[0] * zMult, 0, subTypeOther);
		turnTrack2.setDrop(null);
		turnTrack2.setHasModel(false);
		// TODO This is a guess
		turnTrack2.setRotationCenter(xMult * 6, 0, zMult * 10, 8.5f);

		tracks.add(turnTrack);
		for (int i = 1; i < xArray2.length; i++) {
			tracks.add(new TrackGag(this, xArray2[i] * xMult, 0, zArray2[i] * zMult, 0, subTypeOther));
		}*/
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		// TODO Auto-generated method stub
		return null;
	}

}
