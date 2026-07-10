package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackModelPart;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
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
		HashMap<Vec3i, Float> bedHeights = new HashMap<>();
		HashMap<Vec3i, Float> railHeights = new HashMap<>();
		// Pre-calculated rail bed top face normal dir for further use
		HashMap<Vec3i, Vec3d> topNormals = new HashMap<>();
		HashMap<Vec3i, Integer> yOffset = new HashMap<>();
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

		for (VecYPR cur : path) {
			Vec3d gagPos = cur;

			boolean isFlex = gagPos.distanceTo(start) < flexDist || gagPos.distanceTo(end) < flexDist;

			gagPos = gagPos.add(0, heightOffset, 0);

			for (double q = -horiz; q <= horiz; q += 0.1) {//TODO: need rework
				Vec3d nextUp = VecUtil.fromYawRoll(q, 90 + cur.getYaw(), cur.getRoll());
				int posX = (int) Math.floor(gagPos.x + nextUp.x + placeOff.x);
				int posZ = (int) Math.floor(gagPos.z + nextUp.z + placeOff.z);
				int posY = (int) Math.floor(gagPos.y + nextUp.y + placeOff.y);

				Vec3i gag = new Vec3i(posX, posY, posZ);
				if (!positions.contains(gag)) {
					positions.add(gag);

					Vec3d topFacing = computeTopFaceNormal(-cur.getYaw() - 90, -cur.getPitch(), cur.getRoll());
					double rollDelta;
					boolean rollEffectTile = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.rollEffectTile();
					boolean tileTilt = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.railBlockNormal();
					if(!rollEffectTile) tileTilt = false;

					if(rollEffectTile) {
						double cx = posX;
						double cz = posZ;
						double dx = topFacing.x, dy = topFacing.y, dz = topFacing.z;
						double px = gagPos.x, pz = gagPos.z;

						if (Math.abs(dy) < 1e-5) {
							rollDelta = 0;
						}else {
							rollDelta = ( dx * (px - cx) + dz * (pz - cz) ) / dy;
						}
					}else {//legacy
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

					if(rollEffectTile) {
						if(height + relHeight > 0.9) {
							int offsetInt = (int) Math.floor(height + relHeight + 0.1);
							bedHeights.put(gag, (float) (height + relHeight - offsetInt));
							yOffset.put(gag, (int) (deltaGapPos - relHeight + offsetInt));
						}else {
							bedHeights.put(gag, (float) (height + relHeight));
							yOffset.put(gag, (int) (deltaGapPos - relHeight));
						}
					} else {//legacy, will be dropped
						bedHeights.put(gag, (float) (height + Math.max(0, relHeight - 0.1)));
						yOffset.put(gag, (int) (deltaGapPos - relHeight));
					}
                    railHeights.put(gag, (float) relHeight);
					topNormals.put(gag, tileTilt ? topFacing : null);
				}
				if (isFlex || Math.abs(q) > info.settings.gauge.value()) {
					flexPositions.add(gag);
				}
			}
			if (!isFlex && endOfTrack) {
				mainX = (int) Math.floor(gagPos.x + placeOff.x);
				mainZ = (int) Math.floor(gagPos.z + placeOff.z);
			}
		}

		if (!yOffset.containsKey(new Vec3i(mainX, mainY, mainZ))) {
			// Try a few different offsets
			for (Facing value : Facing.values()) {
				if (yOffset.containsKey(new Vec3i(mainX + value.getXMultiplier(), mainY + value.getYMultiplier(), mainZ + value.getZMultiplier()))) {
					mainX += value.getXMultiplier();
					mainY += value.getYMultiplier();
					mainZ += value.getZMultiplier();
					break;
				}
			}
		}
		if (!yOffset.containsKey(new Vec3i(mainX, mainY, mainZ))) {
			// No luck, code is really borked now.  Throw an exception to help track this.
			TagCompound debug = new TagCompound();
			try {
				TagSerializer.serialize(debug, info);
			} catch (SerializationException e) {
				throw new RuntimeException("Invalid track builder", e);
			}
			throw new RuntimeException("Invalid track builder " + debug);
		}

		Vec3i mainPos = new Vec3i(mainX, yOffset.get(new Vec3i(mainX, mainY, mainZ)), mainZ);
		this.setParentPos(mainPos);
		TrackRail main = new TrackRail(this, mainPos	);
		tracks.add(main);
		main.setRailHeight(railHeights.get(new Vec3i(mainX, mainY, mainZ)));
		main.setBedHeight(bedHeights.get(new Vec3i(mainX, mainY, mainZ)));

		for (Vec3i tilePos : positions) {
			if (tilePos.x == mainX && tilePos.z == mainZ && tilePos.y == mainY) {
				// Skip parent block
				continue;
			}
			TrackBase tg = new TrackGag(this, new Vec3i(tilePos.x, yOffset.get(tilePos), tilePos.z));
			if (flexPositions.contains(tilePos)) {
				tg.setFlexible();
			}
			tg.setRailHeight(railHeights.get(tilePos));
			tg.setBedHeight(bedHeights.get(tilePos));
			tracks.add(tg);
		}
	}

	private Vec3d computeTopFaceNormal(double yawDeg, double pitchDeg, double rollDeg) {
		double yawRad = Math.toRadians(yawDeg);
		double pitchRad = Math.toRadians(pitchDeg);
		double rollRad = Math.toRadians(rollDeg);

		// Compute forward direction vector
		double cosYaw = Math.cos(yawRad);
		double sinYaw = Math.sin(yawRad);
		double cosPitch = Math.cos(pitchRad);
		double sinPitch = Math.sin(pitchRad);
		// pitch > 0 is downhill => forward.y is negative
		Vec3d forward = new Vec3d(cosYaw * cosPitch, -sinPitch, sinYaw * cosPitch).normalize();

		Vec3d upNoRoll;
		if (Math.abs(pitchRad) < 1e-6) {
			upNoRoll = new Vec3d(0, 1, 0);
		} else {
			// Calculate right vector (handle case where forward is nearly parallel to Y axis)
			Vec3d worldUp = new Vec3d(0, 1, 0);
			Vec3d right;
			if (Math.abs(forward.dotProduct(worldUp)) > 0.9999) {
				// Forward almost vertical, use Z axis as temporary reference
				right = new Vec3d(0, 0, 1).crossProduct(forward).normalize();
			} else {
				right = forward.crossProduct(worldUp).normalize();
			}
			upNoRoll = right.crossProduct(forward).normalize();
		}

		if (Math.abs(rollRad) < 1e-6) {
			return upNoRoll;
		}

		// roll > 0 means left higher than right → up direction should rotate around forward by -rollRad
		double cosR = Math.cos(-rollRad);
		double sinR = Math.sin(-rollRad);
		// Rodrigues rotation formula
		Vec3d rotated = upNoRoll.scale(cosR)
				.add(forward.crossProduct(upNoRoll).scale(sinR))
				.add(forward.scale(forward.dotProduct(upNoRoll) * (1 - cosR)));
		return rotated.normalize();
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