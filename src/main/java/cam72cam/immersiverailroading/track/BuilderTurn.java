package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BuilderTurn extends BuilderBase {

	private int radius;
	private TrackDirection direction;

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
		
		this.radius = radius;
		this.direction = type.getDirection();

		
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

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		int xMult = direction == TrackDirection.LEFT ? -1 : 1;
		int xOff = direction == TrackDirection.LEFT ? 0 : 1;
		
		float angleDelta = (float) (90 / (Math.PI * (radius+1)/2));
		
		for (float angle = 0; angle < 90; angle+=angleDelta) {
			double gagX = Math.sin(Math.toRadians(angle)) * (radius+1)+xOff;
			double gagZ = Math.cos(Math.toRadians(angle)) * (radius+1);
			
			data.add(new VecYawPitch(gagX * xMult - xMult * radius, 0, gagZ, (angle + angleDelta/2)*xMult+90));
		}
		
		return data;
	}
}
