package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BuilderTurn extends BuilderBase {

	private int radius;
	private TrackDirection direction;

	public BuilderTurn(World world, int x, int y, int z, EnumFacing rotation, int radius, int quarter, int quarters, TrackDirection direction) {
		super(world, x, y, z, rotation);
		
		this.radius = radius;
		this.direction = direction;

		int xMult = direction == TrackDirection.LEFT ? -1 : 1;
		int zMult = 1;
		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		double hack = -1;
		float angleDelta = (90 / ((float)Math.PI * (radius)/2));
		
		float startAngle = 90 - quarter/4 * 90;
		float endAngle = startAngle - quarters/4 * 90;
		
		if (direction == TrackDirection.LEFT) {
			startAngle = 180 + 90 + quarter/4 * 90;
			endAngle = startAngle + quarters/4 * 90;
		}

		int xPos = (int)(Math.sin(Math.toRadians(startAngle)) * (radius+hack+xMult));
		int zPos = (int)(Math.cos(Math.toRadians(startAngle)) * (radius+hack+zMult));
		float flexAngle = endAngle;
		
		if (direction == TrackDirection.LEFT) {
			float tmp = startAngle;
			startAngle = endAngle;
			endAngle = tmp;
		}
		
		for (float angle = startAngle; angle > endAngle; angle-=angleDelta) {
			int gagX;
			int gagZ;
			boolean isFlex = flexAngle + angleDelta > angle && flexAngle - angleDelta < angle;
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack-0.51));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack-0.51));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+0.5));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+0.5));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1.5));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1.5));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+1.99));
			gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+1.99));
			positions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
			if (isFlex)
				flexPositions.add(Pair.of(gagX+1-xPos, gagZ-zPos));
		}

		
		TrackRail turnTrack = new TrackRail(this, 0, 0, 0, EnumFacing.NORTH, TrackItems.TURN, radius, quarter);
		
		turnTrack.setRotationCenter(-xMult * radius, 0, 0);
		turnTrack.setDirection(direction);
		turnTrack.setTurnQuarters(quarters);
		
		xMult = 1;
		tracks.add(turnTrack);
		for (Pair<Integer, Integer> pair : positions) {
			int gagX = pair.getLeft() * xMult; 
			int gagZ = pair.getRight() * zMult;
			if (gagX == 0 && gagZ == 0) {
				// Skip parent block
				continue;
			}
			/*if (gagX > radius || gagZ > radius) {
				// Skip out of bounds
				continue;
			}*/
			TrackBase tg = new TrackGag(this, gagX, 0, gagZ);
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tracks.add(tg);
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float angleDelta = (90 / ((float)Math.PI * (radius)/2));
		
		if (direction == TrackDirection.RIGHT) {
			
			for (float angle = 45+-angleDelta/2; angle < 90+angleDelta/2; angle+=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * (radius);
				double gagZ = Math.cos(Math.toRadians(angle)) * (radius);
				
				data.add(new VecYawPitch(gagX - radius+1+0.5, 0, gagZ+0.5, Math.min(180, angle+90 + angleDelta/2)));
			}
		} else {
			for (float angle = 45; angle < 90; angle+=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * (radius-1);
				double gagZ = Math.cos(Math.toRadians(angle)) * (radius-1);
				
				data.add(new VecYawPitch(gagX * -1 - -1 * radius+0.5, 0, gagZ-0.5, 180-angle + 90 - angleDelta/2));
			}
		}
		
		return data;
	}
}
