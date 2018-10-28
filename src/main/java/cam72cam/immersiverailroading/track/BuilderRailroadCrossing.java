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
	protected HashSet<Pair<Pair<Integer, Integer>, Float>> positions;

	public BuilderRailroadCrossing(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		if (info.direction == TrackDirection.RIGHT) {
			info.quarter = -info.quarter; 
		}
		
		positions = new HashSet<Pair<Pair<Integer, Integer>, Float>>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		angle = info.quarter/4f * 90;
		
		double actualLength = info.length;
		double horiz = gauge.value() + 2f  * gauge.scale();
		double clamp = 0.17 * gauge.scale();
		
		for (float dist = 0; dist < actualLength; dist += 0.25) {
			Vec3d gagPos = VecUtil.fromYaw(dist, angle);
			for (double q = -horiz; q <= horiz; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + angle);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				double height = (1 - Math.abs((int)q)/horiz)/3 - 0.05;
				height *= gauge.scale();
				height = Math.min(height, clamp);
				
				positions.add(Pair.of(Pair.of(posX, posZ), (float)height));
				if (dist < 3 || dist > actualLength - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
		}
		
		this.setParentPos(new BlockPos(mainX, 0, mainZ));
		TrackRail main = new TrackRail(this, mainX, 0, mainZ, EnumFacing.NORTH, info.type, info.length, info.quarter, info.placementPosition);
		tracks.add(main);
		main.setHeight((float)clamp);
		
		for (Pair<Pair<Integer, Integer>, Float> pair : positions) {
			Pair<Integer, Integer> posPair = pair.getLeft();
			if (posPair.getLeft() == mainX && posPair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, posPair.getLeft(), 0, posPair.getRight());
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tg.setHeight(pair.getRight());
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
