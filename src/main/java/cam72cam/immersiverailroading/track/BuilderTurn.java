package cam72cam.immersiverailroading.track;

import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BuilderTurn extends BuilderBase {

	public BuilderTurn(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		
		int radius = 0;
		switch(type.getType()) {
		case TURN_MEDIUM:
			radius = 9;
			break;
		case TURN_LARGE:
			radius = 19;
			break;
		default:
			return;
		}

		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		
		for (int angle = 0; angle < 90; angle++) {
			int gagX = MathHelper.floor(Math.sin(Math.toRadians(angle)) * radius);
			int gagZ = MathHelper.floor(Math.cos(Math.toRadians(angle)) * radius);

			if (angle > 10 && angle < 80) {
				positions.add(Pair.of(gagX, gagZ));
				positions.add(Pair.of(gagX+1, gagZ-1));
			}

			positions.add(Pair.of(gagX+1, gagZ));
			positions.add(Pair.of(gagX+1, gagZ+1));
			positions.add(Pair.of(gagX+2, gagZ));
		}

		int xMult = type.getDirection() == TrackDirection.LEFT ? -1 : 1;
		int zMult = 1;
		
		TrackRail turnTrack = new TrackRail(this, 0, 0, 0, EnumFacing.NORTH, type);
		
		turnTrack.setRotationCenter(-xMult * radius, 0, 0, radius - 1f);
		
		tracks.add(turnTrack);
		for (Pair<Integer, Integer> pair : positions) {
			int gagX = pair.getLeft() * xMult - xMult * radius; 
			int gagZ = pair.getRight() * zMult;
			if (gagX == 0 && gagZ == 0) {
				// Skip parent block
				continue;
			}
			if (gagX > radius || gagZ > radius) {
				// Skip out of bounds
				continue;
			}
			tracks.add(new TrackGag(this, gagX, 0, gagZ));
		}
	}
}
