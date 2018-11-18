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
		
		double horiz = info.settings.gauge.value();
		if (info.settings.isGradeCrossing) {
			horiz += 2f * info.settings.gauge.scale();
		}
		double clamp = 0.17 * info.settings.gauge.scale();
		
		List<PosStep> path = getPath(0.25);
		PosStep start = path.get(0);
		PosStep end = path.get(path.size()-1);

		int mainX = (int) path.get(path.size()/2).x;
		int mainZ = (int) path.get(path.size()/2).z;

		for (PosStep cur : path) {
			Vec3d gagPos = cur;

			boolean isFlex = gagPos.distanceTo(start) < 3 || gagPos.distanceTo(end) < 3;

			for (double q = -horiz; q <= horiz; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + cur.yaw);
				int posX = (int)(gagPos.x+nextUp.x);
				int posZ = (int)(gagPos.z+nextUp.z);
				double height = 0;
				if (info.settings.isGradeCrossing) {
					height = (1 - Math.abs((int)q)/horiz)/3 - 0.05;
					height *= info.settings.gauge.scale();
					height = Math.min(height, clamp);
				}
				
				double relHeight = gagPos.y % 1;
				if (gagPos.y < 0) {
					relHeight += 1;
				}

				Pair<Integer, Integer> gag = Pair.of(posX, posZ);
				if (!positions.contains(gag)) {
					positions.add(gag);
                    bedHeights.put(gag, (float)(height + Math.max(0, relHeight - 0.1)));
                    railHeights.put(gag, (float) relHeight);
					yOffset.put(gag, (int) (gagPos.y - relHeight));
				}
				if (isFlex || Math.abs(q) > info.settings.gauge.value()) {
					flexPositions.add(gag);
				}
			}
			if (!isFlex && endOfTrack) {
				mainX = (int) gagPos.x;
				mainZ = (int) gagPos.z;
			}
		}

		BlockPos mainPos = new BlockPos(mainX, yOffset.get(Pair.of(mainX, mainZ)), mainZ);
		this.setParentPos(mainPos);
		TrackRail main = new TrackRail(this, mainPos	);
		tracks.add(main);
		main.setRailHeight(railHeights.get(Pair.of(mainX, mainZ)));
		main.setBedHeight(bedHeights.get(Pair.of(mainX, mainZ)));
		
		for (Pair<Integer, Integer> pair : positions) {
			if (pair.getLeft() == mainX && pair.getRight() == mainZ) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, new BlockPos(pair.getLeft(), yOffset.get(pair), pair.getRight()));
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
		
		List<PosStep> points = getPath(info.settings.gauge.scale());
		
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
			data.add(new VecYawPitch(cur.x, cur.y, cur.z, 180-cur.yaw, pitch));
		}
		
		return data;
	}
}