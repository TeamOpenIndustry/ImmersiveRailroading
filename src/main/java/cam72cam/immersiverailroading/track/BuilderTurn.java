package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderTurn extends BuilderBase {

	public BuilderTurn(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		
		int []xArray = null;
		int []zArray = null;
		
		int radius = 0;
		switch(type.getType()) {
		case TURN_MEDIUM:
			xArray = new int[] { 0, 0, 1, 1, 2, 0, 1, 2, 3, 4, 3, 2 };
			zArray = new int[] { 0, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4 };
			radius = 4;
			break;
		case TURN_LARGE:
			xArray = new int[] { 0, 0, 0, 1, 0, 1, 0, 1, 2, 1, 2, 2, 3, 3, 4, 5, 4, 5, 5, 6, 6, 7, 7, 8, 9 };
			zArray = new int[] { 0, 1, 2, 2, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 7, 7, 8, 8, 9, 8, 9, 8, 9, 9, 9 };
			radius = 9;
			break;
		default:
		}

		int xMult = type.getDirection() == TrackDirection.RIGHT ? -1 : 1;
		int zMult = 1;
		
		TrackRail turnTrack = new TrackRail(this, xArray[0] * xMult, 0, zArray[0] * zMult, EnumFacing.SOUTH, type);
		turnTrack.setRotationCenter(xMult * radius, 0, 0, radius - 1f);
		
		tracks.add(turnTrack);
		for (int i = 1; i < xArray.length; i ++) {
			tracks.add(new TrackGag(this, xArray[i] * xMult, 0, zArray[i] * zMult));
		}
	}
}
