package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderTurn extends BuilderBase {

	private float realStartAngle;
	private float startAngle;
	private float endAngle;
	
	private int mainX;
	private int mainZ;

	public BuilderTurn(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		double radius = info.length;

		int xMult = info.direction == TrackDirection.LEFT ? -1 : 1;
		int zMult = 1;
		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		double hack = -0.5;
		float angleDelta = (float) (90 / (Math.PI * radius/2));
		
		
		startAngle = 90 - info.quarter/4f * 90;
		endAngle = startAngle - info.quarters/4f * 90;
		//endAngle = startAngle - 90;
		
		if (info.direction == TrackDirection.LEFT) {
			startAngle = 180 + 90 + info.quarter/4f * 90;
			endAngle = startAngle + info.quarters/4f * 90;
			//endAngle = startAngle + 360;
		}

		int xPos = (int)(Math.sin(Math.toRadians(startAngle)) * (radius+hack+xMult));
		int zPos = (int)(Math.cos(Math.toRadians(startAngle)) * (radius+hack+zMult));
		double xAbsPos = (Math.sin(Math.toRadians(startAngle)) * (radius+hack));
		double zAbsPos = (Math.cos(Math.toRadians(startAngle)) * (radius+hack));
		realStartAngle = startAngle;
		
		if (info.direction == TrackDirection.LEFT) {
			float tmp = startAngle;
			startAngle = endAngle;
			endAngle = tmp;
		}
		
		float flexAngle = 6;
		
		for (float angle = startAngle; angle >= endAngle - angleDelta; angle-=angleDelta) {
			
			for (double q = -gauge.value(); q <= gauge.value(); q+=0.1) {
				int gagX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack+q))+1-xPos;
				int gagZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack+q))-zPos;
				positions.add(Pair.of(gagX, gagZ));
				if (angle > startAngle-flexAngle || angle < endAngle+flexAngle)
					flexPositions.add(Pair.of(gagX, gagZ));
			}
			if (Math.ceil(angle) == Math.ceil((startAngle + endAngle)/2)) {
				mainX = (int)(Math.sin(Math.toRadians(angle)) * (radius+hack))+1-xPos;
				mainZ = (int)(Math.cos(Math.toRadians(angle)) * (radius+hack))-zPos;
			}
		}

		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		
		TrackRail turnTrack = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, TrackItems.TURN, info.length, info.quarter, info.placementPosition);
		
		Vec3d center = new Vec3d(-xAbsPos, 0, -zAbsPos-0.5); 
		center = VecUtil.rotateYaw(center, info.facing.getHorizontalAngle() - 90);
		center = info.placementPosition.add(center);
		
		turnTrack.setRotationCenter(center);
		turnTrack.setDirection(info.direction);
		turnTrack.setTurnQuarters(info.quarters);
		
		xMult = 1;
		tracks.add(turnTrack);
		for (Pair<Integer, Integer> pair : positions) {
			int gagX = pair.getLeft() * xMult; 
			int gagZ = pair.getRight() * zMult;
			if (gagX == mainX && gagZ == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, gagX, 0, gagZ);
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tracks.add(tg);
		}
	}
	
	@Override
	public List<TrackBase> getTracksForRender() {
		return this.tracks;
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		// This is NOT perfect.  It is good enough for now.  
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float radius = info.length;
		
		float angleDelta = (90 / ((float)Math.PI * (radius+1)/2)) * (float)gauge.scale();
		
		double hack = 0.05;
		
		double xPos = Math.floor(Math.sin(Math.toRadians(realStartAngle)) * (radius+hack));
		double zPos = Math.floor(Math.cos(Math.toRadians(realStartAngle)) * (radius+hack));
		
		// Magic numbers
		hack = 0.7 * (gauge.value() - Gauge.STANDARD.value()/2);
		
		if (info.direction == TrackDirection.LEFT) {
			xPos += 1;
			zPos += 1;
		} else {
			xPos -= 1;
		}
		xPos += 1-gauge.scale();
		
		int counter = 0;
			
		for (float angle = startAngle-angleDelta/2; angle > endAngle-angleDelta; angle-=angleDelta) {
			double gagX = Math.sin(Math.toRadians(angle)) * (radius+hack)-xPos;
			double gagZ = Math.cos(Math.toRadians(angle)) * (radius+hack)-zPos;
			float switchAngle = 0;
			float switchOffset = 0;
			if (info.switchState == SwitchState.STRAIGHT) {
				if (info.direction == TrackDirection.RIGHT ) {
					if (angle > startAngle - 4*angleDelta) {
						counter++;
						switchOffset = (4-counter) / 30f * -(float)gauge.scale();
						switchAngle = angleDelta * info.length / 30;
					}
				} else {
					if (angle < endAngle + 4*angleDelta) {
						counter++;
						switchOffset = (counter) / 30f * (float)gauge.scale();
						switchAngle = -angleDelta * info.length / 30;
					}
				}
			}
			if (switchAngle == 0) {
				data.add(new VecYawPitch(gagX, 0, gagZ, angle+90 + angleDelta/2 + switchAngle));
			} else {
				data.add(new VecYawPitch(gagX, 0, gagZ, angle+90 + angleDelta/2, "RAIL_BASE", "RAIL_RIGHT"));
				data.add(new VecYawPitch(gagX + switchOffset, 0, gagZ, angle+90 + angleDelta/2 + switchAngle, "RAIL_LEFT"));
			}
		}

		return data;
	}
}
