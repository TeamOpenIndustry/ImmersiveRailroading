package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.render.rail.RailBaseRender;
import cam72cam.immersiverailroading.util.MathUtil;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class BuilderIterator extends BuilderBase implements IIterableTrack {
	protected HashSet<Pair<Integer, Integer>> positions;
	
	public BuilderIterator(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	public abstract List<PosStep> getPath(double stepSize);

	public BuilderIterator(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos);
		
		positions = new HashSet<Pair<Integer, Integer>>();
		HashMap<Pair<Integer, Integer>, Float> bedHeights = new HashMap<Pair<Integer, Integer>, Float>();
		HashMap<Pair<Integer, Integer>, Float> railHeights = new HashMap<Pair<Integer, Integer>, Float>();
		HashMap<Pair<Integer, Integer>, Integer> yOffset = new HashMap<Pair<Integer, Integer>, Integer>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<Pair<Integer, Integer>>();

		//use to guarantee all slopes look the same no matter what order the points get processed in
		HashMap<Pair<Integer, Integer>, Float> minTracker = new HashMap<>();

		
		double horiz = info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		if (info.settings.isGradeCrossing) {
			horiz += 2f * info.settings.gauge.scale();
		}
		double clamp = 0.17 * info.settings.gauge.scale();
		float heightOffset = (float) MathUtil.trueModulus(info.placementInfo.placementPosition.y, 1);

		List<PosStep> path = getPath(0.25);
		PosStep start = path.get(0);
		PosStep end = path.get(path.size()-1);

		Vec3d placeOff = new Vec3d(
				Math.abs(info.placementInfo.placementPosition.x%1)-0.5,
				0,
                Math.abs(info.placementInfo.placementPosition.z%1)-0.5
		).scale(-1);
		int mainX = (int) Math.round(path.get(path.size()/2).x+placeOff.x);
		int mainZ = (int) Math.round(path.get(path.size()/2).z+placeOff.z);
		int flexDist = (int) Math.max(1, 3 * (0.5 + info.settings.gauge.scale()/2));

		for (PosStep cur : path) {
			Vec3d gagPos = cur;

			boolean isFlex = gagPos.distanceTo(start) < flexDist || gagPos.distanceTo(end) < flexDist;

			gagPos = gagPos.addVector(0, heightOffset, 0);

			for (double q = -horiz; q <= horiz; q+=0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + cur.yaw);
				//TODO: check if this is the cause of rail bed shift, possible rounding of negative floats
				int posX = (int)Math.round(gagPos.x+nextUp.x+placeOff.x);
				int posZ = (int)Math.round(gagPos.z+nextUp.z+placeOff.z);
				double height = 0;
				if (info.settings.isGradeCrossing) {
					height = (1 - Math.abs((int)q)/horiz)/3 - 0.05;
					height *= info.settings.gauge.scale();
					height = Math.min(height, clamp);
				}

				double relHeight = MathUtil.trueModulus(gagPos.y, 1);

				Pair<Integer, Integer> gag = Pair.of(posX, posZ);
				float bedScaleFactor = RailBaseRender.bedScaleFactor(info.settings.gauge);
				if (!positions.contains(gag)) {
					positions.add(gag);
					minTracker.put(gag, (float) gagPos.y);
                    bedHeights.put(gag, getBedHeight((float) height, (float) relHeight, bedScaleFactor));
                    railHeights.put(gag, (float) relHeight);
                    //add buffer space equal to the amount of extra room given to the rail base
					//prevents full blocks of rail base
					yOffset.put(gag,  (int) Math.floor(gagPos.y + bedScaleFactor));
				}

				//smooth slope rendering help
				//force start and end points to be highest priority for reference
				//so that start and end of tracks always line up
				else if(cur == start || cur == end) {
					minTracker.put(gag, -999999f);
					bedHeights.put(gag, getBedHeight((float) height, (float) relHeight, bedScaleFactor));
					railHeights.put(gag, (float) relHeight);
					yOffset.put(gag,  (int) Math.floor(gagPos.y + bedScaleFactor));
				}
				//otherwise prioritize point with lowest y so that it's fairly close to the same on all curves
				else if(gagPos.y < minTracker.get(gag)) {
					minTracker.put(gag, (float) gagPos.y);
					bedHeights.put(gag, getBedHeight((float) height, (float) relHeight, bedScaleFactor));
					railHeights.put(gag, (float) relHeight);
					yOffset.put(gag,  (int) Math.floor(gagPos.y + bedScaleFactor));
				}
				if (isFlex || Math.abs(q) > info.settings.gauge.value()) {
					flexPositions.add(gag);
				}
			}
			if (!isFlex && endOfTrack) {
				mainX = (int) Math.round(gagPos.x+placeOff.x);
				mainZ = (int) Math.round(gagPos.z+placeOff.z);
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

	private float getBedHeight(float height, float relHeight, float bedScaleFactor) {
		return height + relHeight + bedScaleFactor > 1 ? height + relHeight - 1 : height + relHeight;
	}
	
	@Override
	public List<TrackBase> getTracksForRender() {
		return this.tracks;
	}

	private static float delta(float a, float b) {
		float angle = (float) Math.toDegrees(Math.toRadians(a) - Math.toRadians(b));
		if (angle > 180) {
			angle -= 360;
		}
		if (angle<-180) {
			angle += 360;
		}
		return angle;
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();

		double scale = info.settings.gauge.scale();
		List<PosStep> points = getPath(scale);

		boolean switchStraight = info.switchState == SwitchState.STRAIGHT;
		int switchSize = 0;
		TrackDirection direction = info.placementInfo.direction;
		if (switchStraight ) {
			for (int i = 0; i < points.size(); i++) {
				PosStep cur = points.get(i);
				Vec3d flatPos = VecUtil.rotateYaw(cur, -info.placementInfo.yaw);
				if (Math.abs(flatPos.z) >= 0.5 * scale) {
					switchSize = i;
					break;
				}
			}
		}

		for (int i = 0; i < points.size(); i++) {
			PosStep cur = points.get(i);
			PosStep switchPos = cur;
			if (switchStraight ) {
				double switchOffset = 1 - (i / (double)switchSize);
				if (switchOffset > 0) {
					double dist = 0.2 * switchOffset * scale;
					Vec3d offset = VecUtil.fromYaw(dist, cur.yaw + 90 + info.placementInfo.direction.toYaw());
					double offsetAngle = Math.toDegrees(0.2/switchSize); // This line took a whole page of scribbled math
					if (direction == TrackDirection.RIGHT)  {
						offsetAngle = -offsetAngle;
					}
					switchPos = new PosStep(cur.add(offset), cur.yaw + (float)offsetAngle, cur.pitch);
				}
			}
			
			float angle;
			if (points.size() == 1) {
				angle = 0;
			} else if (i+1 == points.size()) {
				PosStep next = points.get(i-1);
				angle = delta(next.yaw, cur.yaw);
				angle *= 2;
			} else if (i == 0) {
				PosStep next = points.get(i+1);
				angle = delta(cur.yaw, next.yaw);
				angle *= 2;
			} else {
				PosStep prev = points.get(i-1);
				PosStep next = points.get(i+1);
				angle = delta(prev.yaw, next.yaw);
			}
			if (angle != 0) {
				if (direction == TrackDirection.RIGHT) {
					data.add(new VecYawPitch(switchPos.x, switchPos.y, switchPos.z, switchPos.yaw, switchPos.pitch, (1 - angle / 180) * (float) info.settings.gauge.scale(), "RAIL_LEFT"));
					data.add(new VecYawPitch(cur.x, cur.y, cur.z, cur.yaw, cur.pitch, (1 + angle / 180) * (float) info.settings.gauge.scale(), "RAIL_RIGHT"));
				} else {
					data.add(new VecYawPitch(cur.x, cur.y, cur.z, cur.yaw, cur.pitch, (1 - angle / 180) * (float) info.settings.gauge.scale(), "RAIL_LEFT"));
					data.add(new VecYawPitch(switchPos.x, switchPos.y, switchPos.z, switchPos.yaw, switchPos.pitch, (1 + angle / 180) * (float) info.settings.gauge.scale(), "RAIL_RIGHT"));
				}
				data.add(new VecYawPitch(cur.x, cur.y, cur.z, cur.yaw, cur.pitch, "RAIL_BASE"));
			} else {
				data.add(new VecYawPitch(cur.x, cur.y, cur.z, cur.yaw, cur.pitch));
			}
		}
		
		return data;
	}
}