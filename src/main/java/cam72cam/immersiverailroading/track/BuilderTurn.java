package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderTurn extends BuilderBase {

	private int mainX;
	private int mainZ;
	public HashSet<Pair<Integer, Integer>> positions;

	public BuilderTurn(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		float flexAngle = 6;
		
		followCurve(info, 0.25f, (double gagX, double gagZ, float angle, float angleDelta, int counter, float startAngle, float endAngle) -> {
			for (double q = -gauge.value(); q <= gauge.value(); q+=0.1) {
				int posX = (int)(gagX + Math.sin(Math.toRadians(angle)) * q);
				int posZ = (int)(gagZ + Math.cos(Math.toRadians(angle)) * q);
				positions.add(Pair.of(posX, posZ));
				if (angle > startAngle-flexAngle || angle < endAngle+flexAngle)
					flexPositions.add(Pair.of(posX, posZ));
			}
			
			if (Math.ceil(angle) == Math.ceil((startAngle + endAngle)/2)) {
				mainX = (int)gagX;
				mainZ = (int)gagZ;
			}
		});

		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		
		TrackRail turnTrack = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, TrackItems.TURN, info.length, info.quarter, info.placementPosition);
		turnTrack.setDirection(info.direction);
		turnTrack.setTurnQuarters(info.quarters);
		
		tracks.add(turnTrack);
		for (Pair<Integer, Integer> pair : positions) {
			int gagX = pair.getLeft(); 
			int gagZ = pair.getRight();
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
		
		followCurve(info, 1, (double gagX, double gagZ, float angle, float angleDelta, int counter, float startAngle, float endAngle) -> {
			float switchAngle = 0;
			float switchOffset = 0;
			if (info.switchState == SwitchState.STRAIGHT) {
				if (info.direction == TrackDirection.LEFT) {
					if (angle > startAngle - 4*angleDelta) {
						switchOffset = (4-counter) / 30f * -(float)gauge.scale();
						switchAngle = angleDelta * info.length / 30;
					}
				} else {
					if (angle < endAngle + 4*angleDelta) {
						switchOffset = ((angle - endAngle)/angleDelta - 4) / 30f * -(float)gauge.scale();
						switchAngle = -angleDelta * info.length / 30;
					}
				}
			}
			if (switchAngle == 0) {
				data.add(new VecYawPitch(gagX, 0, gagZ, angle+90));
			} else {
				data.add(new VecYawPitch(gagX, 0, gagZ, angle+90, "RAIL_RIGHT", "RAIL_BASE"));
				data.add(new VecYawPitch(gagX + switchOffset, 0, gagZ, angle+90 + switchAngle, "RAIL_LEFT"));
			}
		});
		
		if (info.switchState != SwitchState.NONE) {
			double dir = info.direction == TrackDirection.LEFT ? -1 : 1;
			dir = dir * info.gauge.scale();
			double off = 0.2 * dir;
			if (info.switchState == SwitchState.STRAIGHT) {
				 off += 0.2 * dir;
			}
			float angle = info.quarter/4f * 90;
			Vec3d pos = VecUtil.rotateYaw(new Vec3d(off, 0.11 * info.gauge.scale(),  0), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, 180, "RAIL_BASE"));
		}

		return data;
	}
	
	@FunctionalInterface
	public interface RailFunc {
		void accept(double gagX, double gagZ, float angle, float angleDelta, int i, float startAngle, float endAngle);
	}
	
	public static Vec3d followCurve(RailInfo info, float delta, RailFunc fn) {
		float radius = info.length;
		
		float startAngle = 90 - info.quarter/4f * 90;
		float endAngle = startAngle - info.quarters/4f * 90;
		
		if (info.direction == TrackDirection.RIGHT) {
			startAngle = 180 + 90 + info.quarter/4f * 90;
			endAngle = startAngle + info.quarters/4f * 90;
		}
		
		float realStartAngle = startAngle;
		
		float angleDelta = (90 / ((float)Math.PI * (radius)/2)) * (float)info.gauge.scale() * delta;
		
		if (info.direction == TrackDirection.RIGHT) {
			float tmp = startAngle;
			startAngle = endAngle - angleDelta;
			endAngle = tmp;
		}
		float hack = (float) ((1-1/info.gauge.scale()) * angleDelta)/2;
		if (info.direction == TrackDirection.LEFT) {
			startAngle -= hack;
			endAngle += hack;
		} else {
			startAngle += hack;
			endAngle += hack;
		}
		
		radius -= 1;
		
		double xPos = Math.sin(Math.toRadians(realStartAngle)) * (radius);
		double zPos = Math.cos(Math.toRadians(realStartAngle)) * (radius);
		
		if (fn != null) {
			int counter = 0;
			
			for (float angle = startAngle; angle >= endAngle + (startAngle-endAngle)/2; angle-=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * radius - xPos;
				double gagZ = Math.cos(Math.toRadians(angle)) * radius - zPos;
	
				fn.accept(gagX, gagZ, angle, angleDelta, ++counter, startAngle, endAngle);
			}
			
			for (float angle = endAngle; angle <= endAngle + (startAngle-endAngle)/2 + angleDelta; angle+=angleDelta) {
				double gagX = Math.sin(Math.toRadians(angle)) * radius - xPos;
				double gagZ = Math.cos(Math.toRadians(angle)) * radius - zPos;
	
				fn.accept(gagX, gagZ, angle, angleDelta, ++counter, startAngle, endAngle);
			}
		}
		
		return new Vec3d(xPos, 0, zPos);
	}
}
