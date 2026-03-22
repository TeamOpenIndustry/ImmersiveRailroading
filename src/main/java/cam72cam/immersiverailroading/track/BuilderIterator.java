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
	protected HashSet<Pair<Integer, Integer>> positions;
	
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
		HashMap<Pair<Integer, Integer>, Float> bedHeights = new HashMap<>();
		HashMap<Pair<Integer, Integer>, Vec3d> topFacings = new HashMap<>();//TODO: waiting for normal support
		HashMap<Pair<Integer, Integer>, Float> railHeights = new HashMap<>();
		HashMap<Pair<Integer, Integer>, Integer> yOffset = new HashMap<>();
		HashSet<Pair<Integer, Integer>> flexPositions = new HashSet<>();
		
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
		VecYPR start = path.get(0);
		VecYPR end = path.get(path.size()-1);

		Vec3d placeOff = new Vec3d(
				Math.abs(MathUtil.trueModulus(info.placementInfo.placementPosition.x, 1)),
				0,
                Math.abs(MathUtil.trueModulus(info.placementInfo.placementPosition.z, 1))
		);
		int mainX = (int) Math.floor(path.get(path.size() / 2).x + placeOff.x);
		int mainZ = (int) Math.floor(path.get(path.size() / 2).z + placeOff.z);
		int flexDist = (int) Math.max(1, 3 * (0.5 + info.settings.gauge.scale() / 2));

		for (VecYPR cur : path) {
			Vec3d gagPos = cur;

			boolean isFlex = gagPos.distanceTo(start) < flexDist || gagPos.distanceTo(end) < flexDist;

			gagPos = gagPos.add(0, heightOffset, 0);

			for (double q = -horiz; q <= horiz; q += 0.1) {
				Vec3d nextUp = VecUtil.fromYaw(q, 90 + cur.getYaw());
				int posX = (int) Math.floor(gagPos.x + nextUp.x + placeOff.x);
				int posZ = (int) Math.floor(gagPos.z + nextUp.z + placeOff.z);

				double rollDelta;
//				Vec3d topFacing = computeTopFacing(cur.getYaw(), cur.getPitch(), cur.getRoll());
				boolean rollEffectTile = info.settings.rollAndOffsetInfo != null && info.settings.rollAndOffsetInfo.rollEffectTile;

				if(rollEffectTile && cur.getRoll() != 0) {
					double sin = Math.sin(Math.toRadians(cur.getRoll()));
					rollDelta = sin * q;
				}else {
					rollDelta = 0;
				}

				double deltaGapPos = gagPos.y + rollDelta;
				double height = 0;
				if (info.settings.isGradeCrossing) {
					height = 0.306 - Math.abs(Math.round(q)) / (3 * horiz);
					height *= info.settings.gauge.scale();
					height = Math.min(height, clamp);
				}

				double relHeight = deltaGapPos % 1;
				if (deltaGapPos < 0) {//TODO: need updating after tile facing done
					relHeight += 1;
				}

				Pair<Integer, Integer> gag = Pair.of(posX, posZ);
				if (!positions.contains(gag)) {
					positions.add(gag);

					if(rollEffectTile) {
						if(height + relHeight > 0.9) {//if bedHeight > 0.9 we need to correct yOffset
							bedHeights.put(gag, (float) (height + relHeight - 1));
							yOffset.put(gag, (int) (deltaGapPos - relHeight + 1));
						}else {
							bedHeights.put(gag, (float) (height + relHeight));
							yOffset.put(gag, (int) (deltaGapPos - relHeight));
						}
					} else {
						bedHeights.put(gag, (float) (height + Math.max(0, relHeight - 0.1)));
						yOffset.put(gag, (int) (deltaGapPos - relHeight));
					}
                    railHeights.put(gag, (float) relHeight);
//					topFacings.put(gag, topFacing);
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

		if (!yOffset.containsKey(Pair.of(mainX, mainZ))) {
			// Try a few different offsets
			for (Facing value : Facing.values()) {
				if (yOffset.containsKey(Pair.of(mainX + value.getXMultiplier(), mainZ + value.getZMultiplier()))) {
					mainX += value.getXMultiplier();
					mainZ += value .getZMultiplier();
					break;
				}
			}
		}
		if (!yOffset.containsKey(Pair.of(mainX, mainZ))) {
			// No luck, code is really borked now.  Throw an exception to help track this.
			TagCompound debug = new TagCompound();
			try {
				TagSerializer.serialize(debug, info);
			} catch (SerializationException e) {
				throw new RuntimeException("Invalid track builder", e);
			}
			throw new RuntimeException("Invalid track builder " + debug);
		}

		Vec3i mainPos = new Vec3i(mainX, yOffset.get(Pair.of(mainX, mainZ)), mainZ);
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
			TrackBase tg = new TrackGag(this, new Vec3i(pair.getLeft(), yOffset.get(pair), pair.getRight()));
			if (flexPositions.contains(pair)) {
				tg.setFlexible();
			}
			tg.setRailHeight(railHeights.get(pair));
			tg.setBedHeight(bedHeights.get(pair));
//			tg.setTopFacing(topFacings.get(pair));
			tracks.add(tg);
		}
	}

	private Vec3d computeTopFacing(double yawDeg, double pitchDeg, double rollDeg) {//TODO: check axis direction
		double yawRad = Math.toRadians(yawDeg);
		double pitchRad = Math.toRadians(pitchDeg);
		double rollRad = Math.toRadians(rollDeg);

		// direction
		double cosYaw = Math.cos(yawRad);
		double sinYaw = Math.sin(yawRad);
		double cosPitch = Math.cos(pitchRad);
		double sinPitch = Math.sin(pitchRad);
		Vec3d tangent = new Vec3d(cosYaw * cosPitch, -sinPitch, sinYaw * cosPitch);

		// no roll
		Vec3d normalNoRoll = new Vec3d(sinYaw * sinPitch, cosPitch, cosYaw * sinPitch);

		// roll is 0
		if (Math.abs(rollRad) < 1e-6) return normalNoRoll;

		// roll
		double cosR = Math.cos(rollRad);
		double sinR = Math.sin(rollRad);
		double dot = normalNoRoll.dotProduct(tangent);
		Vec3d cross = tangent.crossProduct(normalNoRoll);
		return normalNoRoll.scale(cosR)
				.add(cross.scale(sinR))
				.add(tangent.scale(dot * (1 - cosR)));
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

		boolean correctPartRailPitch = true;//TODO: make this an Graphic option?
		List<Float> correctLeftPitch = new ArrayList<>();
		List<Float> correctRightPitch = new ArrayList<>();

		if (correctPartRailPitch) {
			if (points.size() <= 2 || info.settings.rollAndOffsetInfo == null) {
				correctPartRailPitch = false;
			} else {
				renderScale *= 1.02f;
				double length = points.size() * info.settings.gauge.scale() * info.getTrackModel().spacing;

				Vec3d[] leftPos = new Vec3d[points.size()];
				Vec3d[] rightPos = new Vec3d[points.size()];

				// pre-calculate rail part pos
				for (int i = 0; i < points.size(); i++) {
					VecYPR cur = points.get(i);
					Vec3d pos = new Vec3d(cur.x, cur.y, cur.z);
					leftPos[i] = pos.add(VecUtil.fromYawRoll(
							info.settings.gauge.value() / 2,
							cur.getYaw() - 90,
							cur.getRoll()
					));
					rightPos[i] = pos.add(VecUtil.fromYawRoll(
							info.settings.gauge.value() / 2,
							cur.getYaw() + 90,
							-cur.getRoll()
					));
				}

				float startLeftPitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(length, false) + points.get(0).getPitch();
				float startRightPitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeStart(length, true) + points.get(0).getPitch();
				correctLeftPitch.add(startLeftPitch);
				correctRightPitch.add(startRightPitch);

				for (int i = 1; i < points.size() - 1; i++) {
					float leftPitch = calcPitch(leftPos[i - 1], leftPos[i + 1]);
					float rightPitch = calcPitch(rightPos[i - 1], rightPos[i + 1]);

					correctLeftPitch.add(leftPitch);
					correctRightPitch.add(rightPitch);
				}

				float endLeftPitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(length, false) + points.get(points.size() - 1).getPitch();
				float endRightPitch = (float) info.settings.rollAndOffsetInfo.getRelRollSlopeEnd(length, true) + points.get(points.size() - 1).getPitch();
				correctLeftPitch.add(endLeftPitch);
				correctRightPitch.add(endRightPitch);
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
					switchPos = new VecYPR(cur.add(offset), cur.getYaw() + (float)offsetAngle, cur.getRoll(), cur.getPitch());
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

			//merge situation when angle == 0
            VecYPR vec = new VecYPR(cur, renderScale, TrackModelPart.RAIL_BASE);//TODO:add a track model part which doesnt roll with rails(maybe called STILL_BASE)
            if (direction == TrackDirection.RIGHT) {
                if(correctPartRailPitch) {//correct rail pitch
					cur = cur.withPitch(correctLeftPitch.get(i));//this looks wired but work... caused by how tracks work before
					switchPos = switchPos.withPitch(correctRightPitch.get(i));
                }
                vec.addChild(new VecYPR(switchPos, (1 - angle / 180) * renderScale, TrackModelPart.RAIL_LEFT));
                vec.addChild(new VecYPR(cur, (1 + angle / 180) * renderScale, TrackModelPart.RAIL_RIGHT));
            } else {
                if(correctPartRailPitch) {//correct rail pitch
					switchPos = switchPos.withPitch(correctLeftPitch.get(i));
					cur = cur.withPitch(correctRightPitch.get(i));
                }
                vec.addChild(new VecYPR(cur, (1 - angle / 180) * renderScale, TrackModelPart.RAIL_LEFT));
                vec.addChild(new VecYPR(switchPos, (1 + angle / 180) * renderScale, TrackModelPart.RAIL_RIGHT));
            }
            data.add(vec);
        }

		return data;
	}

	private float calcPitch(Vec3d from, Vec3d to) {
		double dy = to.y - from.y;
		double dh = VecUtil.flatDistance(from, to);
		return (float) -Math.toDegrees(Math.atan2(dy, dh));
	}
}