package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class BuilderIterator extends BuilderBase {
	protected HashSet<Pair<Integer, Integer>> positions;
	
	public BuilderIterator(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}
	
	public static class PosStep extends Vec3d {
		public final float yaw;

		public PosStep(double xIn, double yIn, double zIn, float yaw) {
			super(xIn, yIn, zIn);
			this.yaw = yaw;
		}

		public PosStep(Vec3d orig, float angle) {
			this(orig.x, orig.y, orig.z, angle);
		}
	}
	
	public abstract List<PosStep> getPath(double stepSize);

	public BuilderIterator(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos);
		
		positions = new HashSet<Pair<Integer, Integer>>();
		HashMap<Pair<Integer, Integer>, Float> bedHeights = new HashMap<Pair<Integer, Integer>, Float>();
		HashMap<Pair<Integer, Integer>, Float> railHeights = new HashMap<Pair<Integer, Integer>, Float>();
		HashMap<Pair<Integer, Integer>, Integer> yOffset = new HashMap<Pair<Integer, Integer>, Integer>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();
		
		double horiz = gauge.value();
		if (info.gradeCrossing) {
			horiz += 2f * gauge.scale();
		}
		double clamp = 0.17 * gauge.scale();
		
		Vec3d start = null;
		Vec3d end = null;
		
		for (PosStep cur : getPath(0.25)) {
			if (start == null) {
				start = cur;
			}
			end = cur;
			
			Vec3d gagPos = cur;
			for (double q = -horiz; q <= horiz; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + cur.yaw);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				double height = 0;
				if (info.gradeCrossing) {
					height = (1 - Math.abs((int)q)/horiz)/3 - 0.05;
					height *= gauge.scale();
					height = Math.min(height, clamp);
				}
				
				double relHeight = gagPos.y % 1;
				if (gagPos.y < 0) {
					relHeight += 1;
				}
				
				positions.add(Pair.of(posX, posZ));
				bedHeights.put(Pair.of(posX, posZ), (float)(height + Math.max(0, relHeight - 0.1)));
				railHeights.put(Pair.of(posX, posZ), (float) relHeight);
				yOffset.put(Pair.of(posX, posZ), (int)(gagPos.y - relHeight));
				if (Math.abs(q) > gauge.scale()) {
					flexPositions.add(Pair.of(posX, posZ));
				}
				if (gagPos.distanceTo(start) < 3 || gagPos.distanceTo(start) > info.length - 3) {
					flexPositions.add(Pair.of(posX, posZ));
				}
			}
		}
		
		int mainX = (int) start.x;
		int mainZ = (int) start.z;
		if (endOfTrack) {
			mainX = (int) end.x;
			mainZ = (int) end.z;
		}
		
		this.setParentPos(new BlockPos(mainX, yOffset.get(Pair.of(mainX, mainZ)), mainZ));
		TrackRail main = new TrackRail(this, mainX, yOffset.get(Pair.of(mainX, mainZ)), mainZ, EnumFacing.NORTH, info.type, info.length);
		tracks.add(main);
		main.setRailHeight(railHeights.get(Pair.of(mainX, mainZ)));
		main.setBedHeight(bedHeights.get(Pair.of(mainX, mainZ)));
		
		for (Pair<Integer, Integer> pair : positions) {
			if (pair.getLeft() == mainX && pair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, pair.getLeft(), yOffset.get(pair), pair.getRight());
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tg.setRailHeight(railHeights.get(pair));
			tg.setBedHeight(bedHeights.get(pair));
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
		
		List<PosStep> points = getPath(gauge.scale());
		
		for (int i = 0; i < points.size(); i++) {
			PosStep cur = points.get(i);
			
			float pitch = 0;
			if (points.size() == 1) {
				//NOP
			} else if (i+1 == points.size()) {
				PosStep next = points.get(i-1);
				double ydelt = next.y - cur.y;
				double dist = next.distanceTo(cur);
				pitch = (float) -Math.toDegrees(Math.atan2(ydelt, dist));
			} else if (i == 0) {
				PosStep next = points.get(i+1);
				double ydelt = next.y - cur.y;
				double dist = next.distanceTo(cur);
				pitch = (float) -Math.toDegrees(Math.atan2(ydelt, dist));
			} else {
				PosStep prev = points.get(i-1);
				PosStep next = points.get(i+1);
				double ydelt = next.y - prev.y;
				double dist = next.distanceTo(prev);
				pitch = (float) Math.toDegrees(Math.atan2(ydelt, dist));
			}
			double y = cur.y;
			
			if (y < 0) {
				//HACK
				y += 1;
			}
			data.add(new VecYawPitch(cur.x, y, cur.z, 180-cur.yaw, pitch));
		}
		
		return data;
	}
}