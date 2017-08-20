package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderTurn extends BuilderBase {

	private int radius;
	private TrackDirection direction;

	public BuilderTurn(World world, int x, int y, int z, EnumFacing rotation, TrackType type) {
		super(world, x, y, z, rotation);
		
		int radius = 0;
		switch(type.getType()) {
		case TURN_MEDIUM:
			radius = 20;
			break;
		case TURN_LARGE:
			radius = 40;
			break;
		default:
			return;
		}
		
		this.radius = radius;
		this.direction = type.getDirection();

		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		double hack = -1;
		float angleDelta = (90 / ((float)Math.PI * (radius)/2));
		
		for (float angle = 0; angle < 90; angle+=angleDelta) {
			int gagX;
			int gagZ;
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack-0.51));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack-0.51));
			positions.add(Pair.of(gagX+1, gagZ));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack));
			positions.add(Pair.of(gagX+1, gagZ));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+0.5));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+0.5));
			positions.add(Pair.of(gagX+1, gagZ));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1));
			positions.add(Pair.of(gagX+1, gagZ));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1.5));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1.5));
			positions.add(Pair.of(gagX+1, gagZ));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1.99));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1.99));
			positions.add(Pair.of(gagX+1, gagZ));
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
			/*if (gagX > radius || gagZ > radius) {
				// Skip out of bounds
				continue;
			}*/
			tracks.add(new TrackGag(this, gagX, 0, gagZ));
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float angleDelta = (90 / ((float)Math.PI * (radius)/2));
		
		if (direction == TrackDirection.RIGHT) {
			
			for (float angle = -angleDelta/2; angle < 90+angleDelta/2; angle+=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * (radius);
				double gagZ = Math.cos(Math.toRadians(angle)) * (radius);
				
				data.add(new VecYawPitch(gagX - radius+1+0.5, 0, gagZ+0.5, Math.min(180, angle+90 + angleDelta/2)));
			}
		} else {
			for (float angle = 0; angle < 90; angle+=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * (radius-1);
				double gagZ = Math.cos(Math.toRadians(angle)) * (radius-1);
				
				data.add(new VecYawPitch(gagX * -1 - -1 * radius+0.5, 0, gagZ-0.5, 180-angle + 90 - angleDelta/2));
			}
		}
		
		return data;
	}
}
