package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderStraight extends BuilderBase {
	protected float angle;
	public int mainX;
	public int mainZ;
	protected HashSet<Pair<Integer, Integer>> positions;
	
	public BuilderStraight(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public BuilderStraight(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos);
		
		if (info.direction == TrackDirection.LEFT) {
			info.quarter = -info.quarter; 
		}
		
		positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		angle = info.quarter/4f * 90;
		
		double actualLength = info.length;
		
		for (float dist = 0; dist < actualLength; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -gauge.value(); q <= gauge.value(); q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				positions.add(Pair.of(posX, posZ));
				if (dist < 3 || dist > actualLength - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
			if (endOfTrack) {
				if (Math.ceil(dist) == Math.ceil(actualLength)) {
					mainX = (int) gagPos.x;
					mainZ = (int) gagPos.z;
				}
			} else {
				if (Math.ceil(dist) == Math.ceil(actualLength/2)) {
					mainX = (int) gagPos.x;
					mainZ = (int) gagPos.z;
				}
			}
		}
		
		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		TrackRail main = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, info.type, info.length, info.quarter, info.placementPosition);
		tracks.add(main);
		
		for (Pair<Integer, Integer> pair : positions) {
			if (pair.getLeft() == mainX && pair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, pair.getLeft(), 0, pair.getRight());
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
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		Vec3d pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, 0), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, 0, info.length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (double i = 0; i < info.length-gauge.scale()/2; i+=gauge.scale()) {
			pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, i), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, "RAIL_BASE"));
		}
		return data;
	}
}