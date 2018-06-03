package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderTurnTable extends BuilderBase {
	protected HashSet<Pair<Integer, Integer>> positions;
	private BlockPos offset;
	
	public BuilderTurnTable(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public BuilderTurnTable(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos);
		
		info.quarter = 0;
		
		positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		offset = new BlockPos(0, 0, info.length);
		
		double radius = info.length;
		
		for (double irad = 1; irad <= radius + 1; irad++) {
			for (double angle = 0; angle < 360; angle++) {
				Vec3d gagPos = VecUtil.fromYaw(irad, (float) angle);
				positions.add(Pair.of((int)gagPos.x, (int)gagPos.z));
			}
		}
		
		this.setParentPos(new BlockPos(offset));
		TrackRail main = new TrackRail(this, offset.getX(), offset.getY(), offset.getZ(), EnumFacing.NORTH, info.type, info.length, info.quarter, info.placementPosition);
		tracks.add(main);
		
		for (Pair<Integer, Integer> pair : positions) {
			if (pair.getLeft() == 0 && pair.getRight() == 0) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, pair.getLeft() + offset.getX(), 0, pair.getRight() + offset.getZ());
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
		
		for (float angle = 0; angle < 360; angle +=22.5) {
			Vec3d gagPos = VecUtil.rotateYaw(new Vec3d(0, 0, info.length), angle-90);
			data.add(new VecYawPitch(gagPos.x + offset.getX(), gagPos.y, gagPos.z + offset.getZ(), -angle));
		}
		
		float angle = 360/16.0f * info.tablePos;
		data.add(new VecYawPitch(offset.getX(), offset.getY(), offset.getZ(), -angle, 0, info.length * 2, "RAIL_RIGHT", "RAIL_LEFT"));
		
		return data;
	}
}