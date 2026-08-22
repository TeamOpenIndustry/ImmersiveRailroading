package cam72cam.immersiverailroading.track;

import java.util.*;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.immersiverailroading.util.*;
import cam72cam.mod.math.Matrix3;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.cutter.BlockCutHelper;
import cam72cam.mod.math.Plane;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

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

		HashMap<Vec3i, Float> railHeights = new HashMap<>();// legacy

		// Pre-calculated rail bed top face normal dir for further use
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

		List<VecYPR> path = new ArrayList<>(getPath(0.25));
		Vec3d placementOffset = info.placementInfo.placementPosition;
        path.replaceAll(vecYPR -> vecYPR.add(placementOffset));

		VecYPR start = path.getFirst();
		VecYPR end = path.getLast();

		Vec3i mainPos = new Vec3i(
				(int) Math.floor(path.get(path.size() / 2).x),
				(int) Math.floor(path.get(path.size() / 2).y),
				(int) Math.floor(path.get(path.size() / 2).z)
		);
		int flexDist = (int) Math.max(1, 3 * (0.5 + info.settings.gauge.scale() / 2));

		boolean rollEffectTile = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.rollEffectTile();
		boolean tileTilt = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.railBlockNormal();
		if(!rollEffectTile) tileTilt = false;

		Vec3d bedFacePivotOffset = info.settings.trackFaceTransSetting.getFacePivotOffset(info.getTrackHeight());
		float bedThickness = (float) (info.settings.trackFaceTransSetting.bedThickness() * info.settings.gauge.scale());

		for (int i = 0; i < path.size(); i++) {

			VecYPR cur = path.get(i);
			Vec3d curNormal = cur.toMatrix3().up();
			Vec3d facePivot = cur.add(applyNormalRotation(bedFacePivotOffset.scale(-1), curNormal)).add(bedFacePivotOffset).add(curNormal.scale(bedThickness));

			boolean isFlex = facePivot.distanceTo(start) < flexDist || facePivot.distanceTo(end) < flexDist;

			for (double q = -horiz; q <= horiz; q += 0.1) {
				Vec3d nextUp = VecUtil.fromYawRoll(q, 90 + cur.getYaw(), cur.getRoll());

				int posX = (int) Math.floor(facePivot.x + nextUp.x);
				int posZ = (int) Math.floor(facePivot.z + nextUp.z);
				int posY = (int) Math.floor(facePivot.y + nextUp.y);
				Vec3i gag = new Vec3i(posX, posY, posZ);

                boolean isNewPos = false;
                if(!positions.contains(gag)) {
                    isNewPos = true;
                    positions.add(gag);
                }

                Vec3d topFacing = computeTopFaceNormal(path, i, q);
                double rollPitchDelta;

                if(rollEffectTile) {
                    Vec3d planePoint = new Vec3d(
                            facePivot.x + 0.5,
                            facePivot.y,
                            facePivot.z + 0.5
                    );
                    float localHeight = BlockCutHelper.getCutCenterHeight(new Plane(planePoint.subtract(gag), topFacing));

                    rollPitchDelta = localHeight - (facePivot.y - posY);
                } else {//legacy
                    rollPitchDelta = 0;
                }

                double faceSample = facePivot.y + rollPitchDelta;

                //legacy, a very rough gradeCrossing...
                double crossingHeight = 0;
                if (info.settings.isGradeCrossing) {
                    crossingHeight = 0.306 - Math.abs(Math.round(q)) / (3 * horiz);
                    crossingHeight *= info.settings.gauge.scale();
                    crossingHeight = Math.min(crossingHeight, clamp);
                }

                double relHeight = faceSample % 1;
                if(faceSample == 1) relHeight = 1;// seems we don't need to handle error?

                if (faceSample < 0) {
                    relHeight += 1;
                }

                if(rollEffectTile) {// bedHeight will be the same as railHeight in this case
                    int offsetInt;
                    if(crossingHeight + relHeight > 1) {
                        offsetInt = (int) Math.floor(crossingHeight + relHeight);
                    }else {
                        offsetInt = 0;
                    }

                    // Height for snow and common block rail
                    float heightResult = (float) (crossingHeight + relHeight - offsetInt);
                    trackBlockPositions.add(gag);

                    List<Float> currentBedHeights = allBedHeights.get(gag) != null ? allBedHeights.get(gag) : new ArrayList<>();
                    currentBedHeights.add(heightResult);
                    allBedHeights.put(gag, currentBedHeights);

                    List<Vec3d> currentTopNormals = allTopNormals.get(gag) != null ? allTopNormals.get(gag) : new ArrayList<>();
                    currentTopNormals.add(topFacing);
                    allTopNormals.put(gag, currentTopNormals);

                    List<Vec3d> currentTopPositions = allTopPositions.get(gag) != null ? allTopPositions.get(gag) : new ArrayList<>();
                    currentTopPositions.add(facePivot.subtract(gag));
                    allTopPositions.put(gag, currentTopPositions);

                } else if(isNewPos){// legacy
                    bedHeights.put(gag, (float) (crossingHeight + Math.max(0, relHeight)));
                    railHeights.put(gag, (float) relHeight);
                    trackBlockPositions.add(gag);
                }

                if (isFlex || Math.abs(q) > info.settings.gauge.value()) {
					flexPositions.add(gag);
				}
			}
			if (!isFlex && endOfTrack) {
				mainPos = new Vec3i(
						(int) Math.floor(facePivot.x),
						(int) Math.floor(facePivot.y),
						(int) Math.floor(facePivot.z)
				);
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


		if (!trackBlockPositions.contains(mainPos)) {
			// Try a few different offsets
			for (Facing value : Facing.values()) {
				Vec3i vec = new Vec3i(value.getXMultiplier(), value.getYMultiplier(), value.getZMultiplier());
				if (trackBlockPositions.contains(mainPos.add(vec))) {
					mainPos = mainPos.add(vec);
					break;
				}
			}
		}
		if (!trackBlockPositions.contains(mainPos)) {
			// No luck, code is really borked now.  Throw an exception to help track this.
			TagCompound debug = new TagCompound();
			try {
				TagSerializer.serialize(debug, info);
			} catch (SerializationException e) {
				throw new RuntimeException("Invalid track builder", e);
			}
			throw new RuntimeException("Invalid track builder " + debug);
		}

		this.setParentPos(mainPos);
		TrackRail main = new TrackRail(this, mainPos	);
		tracks.add(main);
		main.setRailHeight(railHeights.get(mainPos));
		main.setBedHeight(bedHeights.get(mainPos));
		main.setBedFace(tileTilt ? planes.get(mainPos) : null);

		for (Vec3i tilePos : positions) {
			if (tilePos.equals(mainPos)) {
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

	public static Vec3d applyNormalRotation(Vec3d offset, Vec3d normal) {
		Vec3d up = normal.normalize();

		Vec3d right = up.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1e-12) {
			right = up.crossProduct(new Vec3d(0, 0, 1));
		}
		right = right.normalize();

		Vec3d forward = right.crossProduct(up).normalize();

		return right.scale(offset.x)
				.add(up.scale(offset.y))
				.add(forward.scale(offset.z));
	}

	private Vec3d computeTopFaceNormal(List<VecYPR> points, int index, double q) {
		VecYPR current = points.get(index);
		Matrix3 base = current.toMatrix3();
		int size = points.size();

		// Early return for invalid cases
		if (size < 2 || info.settings.rollAndOffsetInfo == null) {
			return base.up();
		}

		double totalLength = size * info.settings.gauge.scale() * info.getTrackModel().spacing;

		// Start point
		if (index == 0) {
			float pitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(totalLength, true, q);
			return base.copy().rotateLocalPitch(pitch).up();
		}

		// End point
		if (index == size - 1) {
			float pitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(totalLength, true, q);
			return base.copy().rotateLocalPitch(pitch).up();
		}

		if(Math.abs(q) < 1e-6) {
			return base.up();
		}

		// Middle point – compute local derivatives using neighbors
		VecYPR previous = points.get(index - 1);
		VecYPR next = points.get(index + 1);

		Vec3d prevPos = new Vec3d(previous.x, previous.y, previous.z);
		Vec3d currPos = new Vec3d(current.x, current.y, current.z);
		Vec3d nextPos = new Vec3d(next.x, next.y, next.z);

		Matrix3 prevO = previous.toMatrix3();
		Matrix3 currO = current.toMatrix3();
		Matrix3 nextO = next.toMatrix3();

		Vec3d rightPrev = prevPos.add(prevO.right().scale(q));
		Vec3d rightCurr = currPos.add(currO.right().scale(q));
		Vec3d rightNext = nextPos.add(nextO.right().scale(q));

		Matrix3 mid = Matrix3.fromBasis(
				rightNext.subtract(rightPrev),
				rightCurr.subtract(currPos)
		);

		if (q < 0) {
			mid = Matrix3.fromBasis(mid.forward().scale(-1), mid.right());
		}

		return mid.up();
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
		if(info.settings.pickType != TrackItems.STRAIGHT) {
			//TODO: calculate the scale according to real gap
			renderScale *= 1.005f;
		}

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

		boolean correctPartRailOrientation = true;
		List<Matrix3> correctLeftOrientation = new ArrayList<>();
		List<Matrix3> correctRightOrientation = new ArrayList<>();

		Vec3d[] leftPos;
		Vec3d[] rightPos;

        if (points.size() < 2 || info.settings.rollAndOffsetInfo == null) {
            correctPartRailOrientation = false;
        } else {
            double length = points.size() * info.settings.gauge.scale() * info.getTrackModel().spacing;
            leftPos = new Vec3d[points.size()];
            rightPos = new Vec3d[points.size()];

			// Pre-calculate rail part pos
            for (int i = 0; i < points.size(); i++) {
                VecYPR cur = points.get(i);
                Vec3d pos = new Vec3d(cur.x, cur.y, cur.z);
                Matrix3 o = cur.toMatrix3();

                leftPos[i] = pos.subtract(o.right().scale(info.settings.gauge.value() * 0.5));
                rightPos[i] = pos.add(o.right().scale(info.settings.gauge.value() * 0.5));
            }

			//Start
            Matrix3 startBase = points.getFirst().toMatrix3();

            float startLeftPitch =
                    (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(
                            length, false, info.settings.gauge.value());

            float startRightPitch =
                    (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(
                            length, true, info.settings.gauge.value());

			// TODO: pitch fix only works best when pivot is on rail face yet, can we fix it?
            correctLeftOrientation.add(startBase.copy().rotateLocalPitch(startLeftPitch));
            correctRightOrientation.add(startBase.copy().rotateLocalPitch(startRightPitch));

			//Mid
            for (int i = 1; i < points.size() - 1; i++) {
                Matrix3 leftOrientation = Matrix3.fromBasis(leftPos[i + 1].subtract(leftPos[i - 1]),
                                                            points.get(i).subtract(leftPos[i]));
                Matrix3 rightOrientation = Matrix3.fromBasis(rightPos[i + 1].subtract(rightPos[i - 1]),
                                                             rightPos[i].subtract(points.get(i)));

                correctLeftOrientation.add(rightOrientation);//this is extremely wired but it seems the best way...
                correctRightOrientation.add(leftOrientation);
            }

			//End
            Matrix3 endBase = points.getLast().toMatrix3();

            float endLeftPitch =
                    (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(
                            length, false, info.settings.gauge.value());

            float endRightPitch =
                    (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(
                            length, true, info.settings.gauge.value());

            correctLeftOrientation.add(endBase.copy().rotateLocalPitch(endLeftPitch));
            correctRightOrientation.add(endBase.copy().rotateLocalPitch(endRightPitch));
        }

        for (int i = 0; i < points.size(); i++) {
			VecYPR cur = points.get(i);
			VecYPR switchPos = cur;
			boolean disableSwitchRailCorrection = false;
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
					disableSwitchRailCorrection = true;
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
			VecYPR vec = new VecYPR(cur, renderScale, TrackModelPart.RAIL_BASE);
			//TODO: a track model part which doesnt roll with rails(maybe be something like "RAIL_BASE_NOROLL")
            float leftLen = (1 - angle / 180);
            float rightLen = (1 + angle / 180);
            if (direction == TrackDirection.RIGHT) {
                if (correctPartRailOrientation) {//correct rail part
					cur = cur.withMatrix3(correctLeftOrientation.get(i));
					if(!disableSwitchRailCorrection) switchPos = switchPos.withMatrix3(correctRightOrientation.get(i));
				}
				vec.addChild(new VecYPR(switchPos, leftLen * renderScale, TrackModelPart.RAIL_LEFT));
				vec.addChild(new VecYPR(cur, rightLen * renderScale, TrackModelPart.RAIL_RIGHT));
			} else {
                if (correctPartRailOrientation) {//correct rail part
					if(!disableSwitchRailCorrection) switchPos = switchPos.withMatrix3(correctLeftOrientation.get(i));
					cur = cur.withMatrix3(correctRightOrientation.get(i));
				}
				vec.addChild(new VecYPR(cur, leftLen * renderScale, TrackModelPart.RAIL_LEFT));
				vec.addChild(new VecYPR(switchPos, rightLen * renderScale, TrackModelPart.RAIL_RIGHT));
			}
			data.add(vec);
		}

		return data;
	}
}