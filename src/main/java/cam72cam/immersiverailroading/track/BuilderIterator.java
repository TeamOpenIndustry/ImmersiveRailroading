package cam72cam.immersiverailroading.track;

import java.util.*;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.immersiverailroading.util.BlockPlaneHeight;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.cutter.Plane;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;

public abstract class BuilderIterator extends BuilderBase implements IIterableTrack {
	protected HashSet<Vec3i> positions;

	public BuilderIterator(RailInfo info, World world, Vec3i pos) {
		this(info, world, pos, false);
	}

	//Not sensitive to dynamic stepSize, like physics system
	public abstract List<VecYPR> getPath(double stepSize);

	//Sensitive to dynamic stepSize, return the changed stepSize as well
	public abstract Pair<Double, List<VecYPR>> getPathForRender(double targetStepSize);

	public BuilderIterator(RailInfo info, World world, Vec3i pos, boolean endOfTrack) {
		super(info, world, pos);

		positions = new HashSet<>();

		HashMap<Vec3i, Float> railHeights = new HashMap<>();// we will merge railHeight and bedHeight later

		// Pre-calculated rail bed top face normal dir for further use TODO: merge these 2 to Plane
		HashMap<Vec3i, Plane> planes = new HashMap<>();
		HashMap<Vec3i, Float> bedHeights = new HashMap<>();

		// AVG required
		HashMap<Vec3i, List<Vec3d>> allTopNormals = new HashMap<>();
		HashMap<Vec3i, List<Vec3d>> allTopPositions = new HashMap<>();
		HashMap<Vec3i, List<Float>> allBedHeights = new HashMap<>();

		List<Vec3i> trackBlockPositions = new ArrayList<>();
		HashSet<Vec3i> flexPositions = new HashSet<>();

		double horiz = info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		if (info.settings.isGradeCrossing) {
			horiz += 2f * info.settings.gauge.scale();
		}
		double clamp = 0.17 * info.settings.gauge.scale();
		float heightOffset = (float) ((info.placementInfo.placementPosition.y) % 1);

		List<VecYPR> path = getPath(0.25);
		VecYPR start = path.getFirst();
		VecYPR end = path.getLast();

		Vec3d placeOff = new Vec3d(
				Math.abs(MathUtil.trueModulus(info.placementInfo.placementPosition.x, 1)),
				Math.abs(MathUtil.trueModulus(info.placementInfo.placementPosition.y, 1)),
				Math.abs(MathUtil.trueModulus(info.placementInfo.placementPosition.z, 1))
		);
		int mainX = (int) Math.floor(path.get(path.size() / 2).x + placeOff.x);
		int mainZ = (int) Math.floor(path.get(path.size() / 2).z + placeOff.z);
		int mainY = (int) Math.floor(path.get(path.size() / 2).y + placeOff.y);
		int flexDist = (int) Math.max(1, 3 * (0.5 + info.settings.gauge.scale() / 2));

		boolean rollEffectTile = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.rollEffectTile();
//		boolean tileTilt = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.railBlockNormal();
		boolean tileTilt = rollEffectTile;
		if(!rollEffectTile) tileTilt = false;
		double modelHeight = info.getTrackModel().getHeight();
		float bedThickness = 0.1f;// TODO: config able

		for (int i = 0; i < path.size(); i++) {

			VecYPR cur = path.get(i);
			Vec3d curNormal = Orientation.fromYPR(cur).up;
			Vec3d gagPos = cur.add(curNormal.scale(-modelHeight)).add(0, modelHeight, 0).add(curNormal.scale(bedThickness));

			boolean isFlex = gagPos.distanceTo(start) < flexDist || gagPos.distanceTo(end) < flexDist;

			gagPos = gagPos.add(0, heightOffset, 0);

			for (double q = -horiz; q <= horiz; q += 0.1) {
				Vec3d nextUp = VecUtil.fromYawRoll(q, 90 + cur.getYaw(), cur.getRoll());

				int posX = (int) Math.floor(gagPos.x + nextUp.x + placeOff.x);
				int posZ = (int) Math.floor(gagPos.z + nextUp.z + placeOff.z);
				int posY = (int) Math.floor(gagPos.y + nextUp.y + placeOff.y);
				Vec3i gag = new Vec3i(posX, posY, posZ);

				if (true) {
					boolean isNew = false;
					if(!positions.contains(gag)) {
						isNew = true;
						positions.add(gag);
					}

					Vec3d topFacing = computeTopFaceNormal(path, i, q);
					double rollDelta;

					if(rollEffectTile) {
						Vec3d planePoint = new Vec3d(
								gagPos.x + 0.5,
								gagPos.y,
								gagPos.z + 0.5
						);
						float localHeight = BlockPlaneHeight.calculate(planePoint.subtract(gag), topFacing);

						rollDelta = localHeight - (gagPos.y - posY);
					} else {//legacy
						rollDelta = 0;
					}

					double deltaGapPos = gagPos.y + rollDelta;
					double height = 0;
					if (info.settings.isGradeCrossing) {//legacy, a rough gradeCrossing...
						height = 0.306 - Math.abs(Math.round(q)) / (3 * horiz);
						height *= info.settings.gauge.scale();
						height = Math.min(height, clamp);
					}

					double relHeight = deltaGapPos % 1;
					if (deltaGapPos < 0) {
						relHeight += 1;
					}

					if(rollEffectTile) {// bedHeight will be the same as railHeight in this case
						int offsetInt;
						if(height + relHeight > 1) {
							offsetInt = (int) Math.floor(height + relHeight);
						}else {
							offsetInt = 0;
						}

						// Height for snow and common block rail
						float heightResult = (float) (height + relHeight - offsetInt);
						trackBlockPositions.add(gag);

						// AVG required
						List<Float> currentBedHeights = allBedHeights.get(gag) != null ? allBedHeights.get(gag) : new ArrayList<>();
						currentBedHeights.add(heightResult);
						allBedHeights.put(gag, currentBedHeights);

						List<Vec3d> currentTopNormals = allTopNormals.get(gag) != null ? allTopNormals.get(gag) : new ArrayList<>();
						currentTopNormals.add(topFacing);
						allTopNormals.put(gag, currentTopNormals);

						List<Vec3d> currentTopPositions = allTopPositions.get(gag) != null ? allTopPositions.get(gag) : new ArrayList<>();
						currentTopPositions.add(gagPos.add(placeOff).subtract(gag));
						allTopPositions.put(gag, currentTopPositions);

					} else if(isNew){//legacy, will be dropped
						bedHeights.put(gag, (float) (height + Math.max(0, relHeight - 0.1)));
						railHeights.put(gag, (float) relHeight);
						trackBlockPositions.add(gag);
					}
				}
				if (isFlex || Math.abs(q) > info.settings.gauge.value()) {
					flexPositions.add(gag);
				}
			}
			if (!isFlex && endOfTrack) {
				mainX = (int) Math.floor(gagPos.x + placeOff.x);
				mainY = (int) Math.floor(gagPos.y + placeOff.y);
				mainZ = (int) Math.floor(gagPos.z + placeOff.z);
			}
		}


		for (Map.Entry<Vec3i, List<Float>> entry : allBedHeights.entrySet()) {
			Vec3i gapPos = entry.getKey();

			List<Float> currentHeights = allBedHeights.get(gapPos);
			int count = currentHeights.size();
			float heightSum = 0;
			for (float height : currentHeights) {
				heightSum += height;
			}
			float averageBedHeight = heightSum / count;

			List<Vec3d> currentTopNormals = allTopNormals.get(gapPos);
			int countTopNormal = currentTopNormals.size();
			Vec3d topNormalSum = Vec3d.ZERO;
			for (Vec3d topNormal : currentTopNormals) {
				topNormalSum = topNormalSum.add(topNormal);
			}
			Vec3d avgTopNormal = topNormalSum.scale(1d / countTopNormal);

			List<Vec3d> currentTopPositions = allTopPositions.get(gapPos);
			int positionCount = currentTopPositions.size();
			Vec3d topPositionSum = Vec3d.ZERO;
			for (Vec3d position : currentTopPositions) {
				topPositionSum = topPositionSum.add(position);
			}
			Vec3d avgTopPosition = topPositionSum.scale(1d / positionCount);

			// put
			if(avgTopNormal.y < 0) averageBedHeight = -averageBedHeight;
			bedHeights.put(gapPos, averageBedHeight);
			railHeights.put(gapPos, averageBedHeight);
			planes.put(gapPos, new Plane(new Vec3d(avgTopPosition.x, avgTopPosition.y, avgTopPosition.z), avgTopNormal.scale(-1)));
		}


		if (!trackBlockPositions.contains(new Vec3i(mainX, mainY, mainZ))) {
			// Try a few different offsets
			for (Facing value : Facing.values()) {
				if (trackBlockPositions.contains(new Vec3i(mainX + value.getXMultiplier(), mainY + value.getYMultiplier(), mainZ + value.getZMultiplier()))) {
					mainX += value.getXMultiplier();
					mainY += value.getYMultiplier();
					mainZ += value.getZMultiplier();
					break;
				}
			}
		}
		if (!trackBlockPositions.contains(new Vec3i(mainX, mainY, mainZ))) {
			// No luck, code is really borked now.  Throw an exception to help track this.
			TagCompound debug = new TagCompound();
			try {
				TagSerializer.serialize(debug, info);
			} catch (SerializationException e) {
				throw new RuntimeException("Invalid track builder", e);
			}
			throw new RuntimeException("Invalid track builder " + debug);
		}

		Vec3i mainPos = new Vec3i(mainX, mainY, mainZ);
		this.setParentPos(mainPos);
		TrackRail main = new TrackRail(this, mainPos	);
		tracks.add(main);
		main.setRailHeight(railHeights.get(new Vec3i(mainX, mainY, mainZ)));
		main.setBedHeight(bedHeights.get(new Vec3i(mainX, mainY, mainZ)));
		main.setBedFace(tileTilt ? planes.get(new Vec3i(mainX, mainY, mainZ)) : null);

		for (Vec3i tilePos : positions) {
			if (tilePos.x == mainX && tilePos.z == mainZ && tilePos.y == mainY) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, new Vec3i(tilePos.x, tilePos.y, tilePos.z));
			if (flexPositions.contains(tilePos)) {
				tg.setFlexible();
			}
			tg.setRailHeight(railHeights.get(tilePos));
			tg.setBedHeight(bedHeights.get(tilePos));
			tg.setBedFace(tileTilt ? planes.get(tilePos) : null);
			tracks.add(tg);
		}
	}

	private Vec3d computeTopFaceNormal(List<VecYPR> points, int index, double q) {
		VecYPR current = points.get(index);
		Orientation base = Orientation.fromYPR(current);
		int size = points.size();

		// Early return for invalid cases
		if (size < 2 || info.settings.rollAndOffsetInfo == null) {
			return base.up;
		}

		double totalLength = size * info.settings.gauge.scale() * info.getTrackModel().spacing;

		// Start point
		if (index == 0) {
			float pitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(totalLength, true, q);
			return base.rotatePitch(pitch).up;
		}

		// End point
		if (index == size - 1) {
			float pitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(totalLength, true, q);
			return base.rotatePitch(pitch).up;
		}

		// Middle point – compute local derivatives using neighbors
		VecYPR previous = points.get(index - 1);
		VecYPR next = points.get(index + 1);

		Vec3d prevPos = new Vec3d(previous.x, previous.y, previous.z);
		Vec3d currPos = new Vec3d(current.x, current.y, current.z);
		Vec3d nextPos = new Vec3d(next.x, next.y, next.z);

		Orientation prevO = Orientation.fromYPR(previous);
		Orientation currO = Orientation.fromYPR(current);
		Orientation nextO = Orientation.fromYPR(next);

		Vec3d rightPrev = prevPos.add(prevO.right.scale(q));
		Vec3d rightCurr = currPos.add(currO.right.scale(q));
		Vec3d rightNext = nextPos.add(nextO.right.scale(q));

		Orientation mid = new Orientation(
				rightNext.subtract(rightPrev),
				rightCurr.subtract(currPos)
		);

		if (q < 0) {
			mid = new Orientation(mid.forward.scale(-1), mid.right);
		}

		return mid.up;
	}

	@Override
	public List<TrackBase> getTracksForRender() {
		return super.getTracksForRender();
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
	public List<VecYPR> getRenderData() {
		List<VecYPR> data = new ArrayList<>();

		double scale = info.settings.gauge.scale();
		Pair<Double, List<VecYPR>> pair = getPathForRender(scale * info.getTrackModel().spacing);
		List<VecYPR> points = pair.getRight();
		float renderScale = (float) (pair.getLeft() / info.getTrackModel().spacing);
		renderScale *= 1.005f;//Avoid some gaps

		boolean switchStraight = info.switchState == SwitchState.STRAIGHT;
		int switchSize = 0;
		TrackDirection direction = info.placementInfo.direction;
		if (switchStraight ) {
			for (int i = 0; i < points.size(); i++) {
				VecYPR cur = points.get(i);
				Vec3d flatPos = VecUtil.rotateYaw(cur, -info.placementInfo.yaw);
				if (Math.abs(flatPos.z) >= 0.5 * scale) {
					switchSize = i;
					break;
				}
			}
		}

		boolean correctPartRailOrientatio = true;
		List<Orientation> correctLeftOrientation = new ArrayList<>();
		List<Orientation> correctRightOrientation = new ArrayList<>();

		Vec3d[] leftPos = null;
		Vec3d[] rightPos = null;

		if (correctPartRailOrientatio) {
			if (points.size() < 2 || info.settings.rollAndOffsetInfo == null) {
				correctPartRailOrientatio = false;
			} else {
				renderScale *= 1.02f;
				double length = points.size() * info.settings.gauge.scale() * info.getTrackModel().spacing;
				leftPos = new Vec3d[points.size()];
				rightPos = new Vec3d[points.size()];

				// Pre-calculate rail part pos
				for (int i = 0; i < points.size(); i++) {
					VecYPR cur = points.get(i);
					Vec3d pos = new Vec3d(cur.x, cur.y, cur.z);
					Orientation o = Orientation.fromYPR(cur);

					leftPos[i] =
							pos.subtract(o.right.scale(info.settings.gauge.value() * 0.5));

					rightPos[i] =
							pos.add(o.right.scale(info.settings.gauge.value() * 0.5));
				}

				//Start
				Orientation startBase = Orientation.fromYPR(points.getFirst());

				float startLeftPitch =
						(float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(
								length, false, info.settings.gauge.value());

				float startRightPitch =
						(float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(
								length, true, info.settings.gauge.value());

				correctLeftOrientation.add(startBase.rotatePitch(startLeftPitch));
				correctRightOrientation.add(startBase.rotatePitch(startRightPitch));

				//Mid
				for (int i = 1; i < points.size() - 1; i++) {
					Orientation leftOrientation = new Orientation(leftPos[i+1].subtract(leftPos[i-1]), points.get(i).subtract(leftPos[i]));
					Orientation rightOrientation = new Orientation(rightPos[i+1].subtract(rightPos[i-1]), rightPos[i].subtract(points.get(i)));

					correctLeftOrientation.add(rightOrientation);//this is extremely wired but it seems the best way...
					correctRightOrientation.add(leftOrientation);
				}

				//End
				Orientation endBase = Orientation.fromYPR(points.getLast());

				float endLeftPitch =
						(float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(
								length, false, info.settings.gauge.value());

				float endRightPitch =
						(float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(
								length, true, info.settings.gauge.value());

				correctLeftOrientation.add(endBase.rotatePitch(endLeftPitch));
				correctRightOrientation.add(endBase.rotatePitch(endRightPitch));
			}
		}

		for (int i = 0; i < points.size(); i++) {
			VecYPR cur = points.get(i);
			VecYPR switchPos = cur;
			if (switchStraight ) {
				double switchOffset = 1 - (i / (double)switchSize);
				if (switchOffset > 0) {
					double dist = 0.2 * switchOffset * scale * info.getTrackModel().spacing;
					Vec3d offset = VecUtil.fromYaw(dist, cur.getYaw() + 90 + info.placementInfo.direction.toYaw());
					double offsetAngle = Math.toDegrees(0.2/switchSize); // This line took a whole page of scribbled math
					if (direction == TrackDirection.RIGHT)  {
						offsetAngle = -offsetAngle;
					}
					switchPos = new VecYPR(cur.add(offset), cur.getYaw() + (float)offsetAngle, cur.getPitch(), cur.getRoll());
				}
			}

			float angle;
			if (points.size() == 1) {
				angle = 0;
			} else if (i+1 == points.size()) {
				VecYPR next = points.get(i-1);
				angle = delta(next.getYaw(), cur.getYaw());
				angle *= 2;
			} else if (i == 0) {
				VecYPR next = points.get(i+1);
				angle = delta(cur.getYaw(), next.getYaw());
				angle *= 2;
			} else {
				VecYPR prev = points.get(i-1);
				VecYPR next = points.get(i+1);
				angle = delta(prev.getYaw(), next.getYaw());
			}

			//Merge situation when angle == 0
			VecYPR vec = new VecYPR(cur, renderScale, TrackModelPart.RAIL_BASE);//TODO:add a track model part which doesnt roll with rails(maybe be something like "RAIL_BASE_NOROLL")
			if (direction == TrackDirection.RIGHT) {
				float leftLen = (1 - angle / 180);
				float rightLen = (1 + angle / 180);
				if(correctPartRailOrientatio) {//correct rail part
					cur = cur.withOrientation(correctLeftOrientation.get(i));
					switchPos = switchPos.withOrientation(correctRightOrientation.get(i));
				}
				vec.addChild(new VecYPR(switchPos, leftLen * renderScale, TrackModelPart.RAIL_LEFT));
				vec.addChild(new VecYPR(cur, rightLen * renderScale, TrackModelPart.RAIL_RIGHT));
			} else {
				float leftLen = (1 - angle / 180);
				float rightLen = (1 + angle / 180);
				if(correctPartRailOrientatio) {//correct rail part
					switchPos = switchPos.withOrientation(correctLeftOrientation.get(i));
					cur = cur.withOrientation(correctRightOrientation.get(i));
				}
				vec.addChild(new VecYPR(cur, leftLen * renderScale, TrackModelPart.RAIL_LEFT));
				vec.addChild(new VecYPR(switchPos, rightLen * renderScale, TrackModelPart.RAIL_RIGHT));
			}
			data.add(vec);
		}

		return data;
	}
}