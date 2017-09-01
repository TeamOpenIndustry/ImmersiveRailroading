package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderStraight extends BuilderBase {
	private float angle;
	private int mainX;
	private int mainZ;

	public BuilderStraight(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		HashSet<Pair<Integer, Integer>> positions = new HashSet<Pair<Integer, Integer>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		angle = info.quarter/4f * 90;
		
		for (float dist = 0; dist < info.length; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -1.5; q <= 1.5; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				positions.add(Pair.of(posX, posZ));
				if (dist < 3 || dist > info.length - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
			if (Math.ceil(dist) == Math.ceil(info.length/2)) {
				mainX = (int) gagPos.x;
				mainZ = (int) gagPos.z;
			}
		}
		
		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		
		TrackRail main = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, TrackItems.STRAIGHT, info.length, info.quarter, info.horizOff);
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
	public BlockPos getRenderOffset() {
		return convertRelativePositions(mainX, 0, mainZ, EnumFacing.NORTH);
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		data.add(new VecYawPitch(-0.5, 0, 0, -angle, 0, info.length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (int i = 0; i < info.length; i++) {
			Vec3d pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, i), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, "RAIL_BASE"));
		}
		return data;
	}
}