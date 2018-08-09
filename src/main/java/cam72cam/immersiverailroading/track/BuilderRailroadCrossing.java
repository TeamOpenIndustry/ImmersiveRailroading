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

public class BuilderRailroadCrossing extends BuilderBase {
	protected float angle;
	public int mainX;
	public int mainZ;
	protected HashSet<Pair<Float, Pair<Integer, Integer>>> positions;

	public BuilderRailroadCrossing(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		if (info.direction == TrackDirection.RIGHT) {
			info.quarter = -info.quarter; 
		}
		
		positions = new HashSet<Pair<Float, Pair<Integer, Integer>>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		angle = info.quarter/4f * 90;
		
		double actualLength = info.length;
		
		for (float dist = 0; dist < actualLength; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -gauge.value(); q <= gauge.value(); q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + angle);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				positions.add(Pair.of(0.18f * (float) gauge.scale(), Pair.of(posX, posZ)));
				if (dist < 3 || dist > actualLength - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
		}
		
		for (float dist = 0; dist < actualLength; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -gauge.value() - 1f; q <= gauge.value() + 1f; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + angle);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				if (!positions.contains(Pair.of(0.18f * (float) gauge.scale(), Pair.of(posX, posZ)))) {
					positions.add(Pair.of(0.12f * (float) gauge.scale(), Pair.of(posX, posZ)));
					if (dist < 3 || dist > actualLength - 3) {
						flexPositions.add(Pair.of(posX, posZ));
					}
				}
			}
		}
		
		for (float dist = 0; dist < actualLength; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -gauge.value() - 2f; q <= gauge.value() + 2f; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + angle);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				if (!positions.contains(Pair.of(0.18f * (float) gauge.scale(), Pair.of(posX, posZ))) && !positions.contains(Pair.of(0.12f * (float) gauge.scale(), Pair.of(posX, posZ)))) {
					positions.add(Pair.of(0.04f * (float) gauge.scale(), Pair.of(posX, posZ)));
					if (dist < 3 || dist > actualLength - 3) {
						flexPositions.add(Pair.of(posX, posZ));
					}
				}
			}
		}
		
		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		TrackRail main = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, info.type, info.length, info.quarter, info.placementPosition);
		tracks.add(main);
		main.setHeight(0.18f * (float) gauge.scale());
		
		for (Pair<Float, Pair<Integer, Integer>> pair : positions) {
			Pair<Integer, Integer> posPair = pair.getRight();
			if (posPair.getLeft() == mainX && posPair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, posPair.getLeft(), 0, posPair.getRight());
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tg.setHeight(pair.getLeft());
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
		
		Vec3d pos = VecUtil.rotateYaw(new Vec3d(0, 0, info.length/2.0-0.5), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, 0, info.length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		double trackOffset = (1-info.gauge.scale())/4;
		
		for (double i = -trackOffset; i < info.length - trackOffset; i+=gauge.scale()) {
			pos = VecUtil.rotateYaw(new Vec3d(0, 0, i-0.25), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, "RAIL_BASE"));
		}
		return data;
	}
}
