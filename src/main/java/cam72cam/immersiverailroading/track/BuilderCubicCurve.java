package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.RollAndOffsetInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BuilderCubicCurve extends BuilderIterator {
	private List<BuilderBase> subBuilders;

	public BuilderCubicCurve(RailInfo info, World world, Vec3i pos) {
		this(info, world, pos, false);
	}

	@Override
	public List<BuilderBase> getSubBuilders() {
		return subBuilders;
	}

	public BuilderCubicCurve(RailInfo info, World world, Vec3i pos, boolean endOfTrack) {
		super(info, world, pos, endOfTrack);
		CubicCurve curve = getCurve();
		List<CubicCurve> subCurves = curve.subsplit((int) (101 * 2 * 3.1415f / 4));//158.64575
		if (subCurves.size() > 1) {
			subBuilders = new ArrayList<>();

			List<RollAndOffsetInfo> subRollAndOffsetInfos = info.settings.rollAndOffsetInfo == null
															? null
															: info.settings.rollAndOffsetInfo.subSplit(RollAndOffsetInfo.toRange(subCurves), true);

            for (int i = 0; i < subCurves.size(); i++) {
				CubicCurve subCurve = subCurves.get(i);
				// main pos -> subCurve's start pos
				Vec3d relOff = info.placementInfo.placementPosition.add(subCurve.p1);
				Vec3i relPos = new Vec3i(relOff);
				Vec3i sPos = pos.add(relPos);
				// The block remainder of curve position, with the subCurve move to origin block included
				Vec3d delta = relOff.subtract(relPos).subtract(subCurve.p1);
				//delta = delta.subtract(new Vec3i(delta)); // Relative position within the block
				PlacementInfo startPos = new PlacementInfo(subCurve.p1.add(delta), info.placementInfo.direction, subCurve.angleStart(), subCurve.ctrl1.add(delta));
				PlacementInfo endPos   = new PlacementInfo(subCurve.p2.add(delta), info.placementInfo.direction, subCurve.angleStop(), subCurve.ctrl2.add(delta));

				int finalI = i;
				RailInfo subInfo = new RailInfo(info.settings.with(b -> {
					b.type = TrackItems.CUSTOM;
					if(subRollAndOffsetInfos != null) {
						b.rollAndOffsetInfo = subRollAndOffsetInfos.get(finalI);
					}
				}), startPos, endPos, SwitchState.NONE, SwitchState.NONE, 0);

				BuilderCubicCurve subBuilder = new BuilderCubicCurve(subInfo, world, sPos);
				if (!subBuilders.isEmpty()) {
					for (TrackBase track : subBuilder.tracks) {
						if (track instanceof TrackRail) {
							track.overrideParent(subBuilders.getFirst().getParentPos());
						}
					}
				} else {
					tracks = subBuilder.tracks;
				}
				subBuilders.add(subBuilder);
			}
		}
	}

	private HashMap<Double, Pair<Double, List<VecYPR>>> cache;

	public CubicCurve getCurve() {
		Vec3d nextPos = new Vec3d(new Vec3i(VecUtil.fromYaw(info.settings.length, info.placementInfo.yaw + 45)));

		boolean isDefault = info.customInfo.placementPosition.equals(info.placementInfo.placementPosition);
		if (!isDefault) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition);
		}

		double ctrlGuess = nextPos.length()/2 * Math.max(0.1, info.settings.curvosity);
		float angle = info.placementInfo.yaw;

		float angle2 = angle + 180;

		if (!isDefault) {
			angle2 = info.customInfo.yaw;
		}

		Vec3d ctrl1 = VecUtil.fromYaw(ctrlGuess, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(ctrlGuess, angle2));

		CubicCurve adjusted = new CubicCurve(Vec3d.ZERO, ctrl1, ctrl2, nextPos, 0, 1)
				                      .linearize(info.settings.smoothing);
		ctrl1 = adjusted.ctrl1;
		ctrl2 = adjusted.ctrl2;

		if (info.placementInfo.control != null) {
			ctrl1= info.placementInfo.control.subtract(info.placementInfo.placementPosition);
		}
		if (info.customInfo.control != null && !isDefault) {
            ctrl2 = info.customInfo.control.subtract(info.placementInfo.placementPosition);
		}

		return new CubicCurve(Vec3d.ZERO, ctrl1, ctrl2, nextPos, 0, 1);
	}

	@Override
    public List<VecYPR> getPath(double stepSize) {
		return getPathForRender(stepSize).getRight();
	}

	@Override
	public Pair<Double, List<VecYPR>> getPathForRender(double targetStepSize) {
		if (cache == null) {
			cache = new HashMap<>();
		}

		if (cache.containsKey(targetStepSize)) {
			return cache.get(targetStepSize);
		}

		List<VecYPR> res = new ArrayList<>();
		CubicCurve curve = getCurve();

		// HACK for super long curves
		// Skip the super long calculation since it'll be overridden anyways
		List<CubicCurve> curves =  curve.subsplit(200);
		curve = curves.getFirst();
		RollAndOffsetInfo rollAndOffsetInfo;
		rollAndOffsetInfo = info.settings.rollAndOffsetInfo == null
							? null
							: info.settings.rollAndOffsetInfo.subSplit(RollAndOffsetInfo.toRange(curves), true).getFirst();
		RollAndOffsetInfo.RollAndVertOffsetAlignType type = rollAndOffsetInfo == null
												 ? RollAndOffsetInfo.RollAndVertOffsetAlignType.MID
												 : rollAndOffsetInfo.rollOffsetType();

		double length = curve.lengthWithCache(1000);
		int count = (int) (length / targetStepSize);//Adapt the length
		double mod = (length / targetStepSize) % 1;
		if(mod > 0.6){
			count += 1;
		}
		double stepSize = length / count;
		if (count * 3 > 1000){//There may be over 1 pieces in the same cache section, rebuild cache
			curve.lengthWithCache(count * 3);
		}

		List<PosRollOffset> posRollOffsets = curve.toList(stepSize, rollAndOffsetInfo);
		List<Vec3d> points = PosRollOffset.getPoints(posRollOffsets);
		List<Double> rolls = PosRollOffset.getRolls(posRollOffsets);
		List<Double> yOffsets = PosRollOffset.getYOffsets(posRollOffsets);
		List<Double> zOffsets = PosRollOffset.getZOffsets(posRollOffsets);
		List<Vec3d> originPoints = new ArrayList<>();
		for (Vec3d p : points) {
			originPoints.add(new Vec3d(p.x, p.y, p.z));
		}

		if(count == 0){//Meaning stepSize must be NaN, caused by curve length == 0
			stepSize = targetStepSize;
		}

		double gaugeScale = info.settings.gauge.scale();//rescale from Standard Gauge
		double gauge = info.settings.gauge.value();

		// Vertical offset mode 1
		if(rollAndOffsetInfo != null && !rollAndOffsetInfo.offsetVertByNormal()) {
			for(int i = 0; i < points.size(); i++) {
				Vec3d p = points.get(i);
				Vec3d newP = new Vec3d(p.x, p.y + yOffsets.get(i) * gaugeScale, p.z);
				points.set(i, newP);
			}
		}

		// Horizontal offset
		boolean correctYaw = false;
		for(int i = 0; i < points.size(); i++) {
			float yaw;
			if (points.size() == 1) {
				yaw = info.placementInfo.yaw;
			} else if (i == points.size()-1) {
				yaw = curve.angleStop();
			} else if (i == 0) {
				yaw = curve.angleStart();
			} else {
				yaw = VecUtil.toYaw(originPoints.get(i+1).subtract(originPoints.get(i-1)));
			}

			if(zOffsets.get(i) != 0) {// The side effect of too much offset is that some dot spacing becomes less uniform, and should we offset points in toList?
				correctYaw = true;
				// Can we still calculate this if pitch is 90?
				Vec3d horizontalOffset = VecUtil.fromYaw(zOffsets.get(i), yaw - 90).scale(gaugeScale / gauge);//zOffset scale
				points.set(i, points.get(i).add(horizontalOffset));
			}
		}

		// Vertical offset mode 2, we have to do this before final ypr calculating and after horizontal offset done
		if(rollAndOffsetInfo != null && rollAndOffsetInfo.offsetVertByNormal()) {
			List<Vec3d> newPosList = new ArrayList<>();
			for(int i = 0; i < points.size(); i++) {
				Vec3d p = points.get(i);
				float yaw;
				float pitch;
				float roll = rolls.get(i).floatValue();
				if (points.size() == 1) {
					yaw = info.placementInfo.yaw;
					pitch = 0;
				} else if (i == points.size()-1) {
					Vec3d next = points.get(i-1);
					pitch = (float) Math.toDegrees(Math.atan2(next.y - p.y, VecUtil.flatDistance(next, p)));
					yaw = curve.angleStop();
					if(correctYaw) yaw += (float) rollAndOffsetInfo.getZOffsetSlopeEnd(length);
				} else if (i == 0) {
					Vec3d next = points.get(i+1);
					pitch = (float) -Math.toDegrees(Math.atan2(next.y - p.y, VecUtil.flatDistance(next, p)));
					yaw = curve.angleStart() ;
					if(correctYaw) yaw += (float) rollAndOffsetInfo.getZOffsetSlopeStart(length);
				} else {
					Vec3d prev = points.get(i-1);
					Vec3d next = points.get(i+1);
					pitch = (float) -Math.toDegrees(Math.atan2(next.y - prev.y, VecUtil.flatDistance(next, prev)));
					yaw = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
				}
				Orientation orientation = Orientation.fromYPR(yaw, pitch, roll);
				Vec3d newP = p.add(orientation.up.scale(yOffsets.get(i) * gaugeScale));
				newPosList.add(newP);
			}
			for(int i = 0; i < points.size(); i++) {
				points.set(i, newPosList.get(i));
			}
		}

		// RollAndVertOffsetAlignType offset override
		switch (type) {
			case MID:
				break;
			case LEFT:
				for(int i = 0; i < points.size(); i++) {
					Vec3d p = points.get(i);
					Vec3d newP;
					if(!rollAndOffsetInfo.degreeMode()) {//superelevision scale
						newP = new Vec3d(p.x, p.y - rolls.get(i) * gaugeScale * 0.01 * 0.5, p.z);
					} else {
						newP = new Vec3d(p.x, p.y - Math.sin(Math.toRadians(rolls.get(i))) * gauge * 0.5, p.z);
					}
					points.set(i, newP);
				}
				break;
			case RIGHT:
				for(int i = 0; i < points.size(); i++) {
					Vec3d p = points.get(i);
					Vec3d newP;
					if(!rollAndOffsetInfo.degreeMode()) {//superelevision scale
						newP = new Vec3d(p.x, p.y + rolls.get(i) * gaugeScale * 0.01 * 0.5, p.z);
					} else {
						newP = new Vec3d(p.x, p.y + Math.sin(Math.toRadians(rolls.get(i))) * gauge * 0.5, p.z);
					}
					points.set(i, newP);
				}
				break;
		}

		// Calculate final YPR
		for(int i = 0; i < points.size(); i++) {
			Vec3d p = points.get(i);
			float yaw;
			float pitch;
			if (points.size() == 1) {
				yaw = info.placementInfo.yaw;
				pitch = 0;
			} else if (i == points.size()-1) {
				Vec3d next = points.get(i-1);
 				pitch = (float) Math.toDegrees(Math.atan2(next.y - p.y, VecUtil.flatDistance(next, p)));
				yaw = curve.angleStop();
				if(correctYaw) yaw += (float) rollAndOffsetInfo.getZOffsetSlopeEnd(length);
			} else if (i == 0) {
				Vec3d next = points.get(i+1);
				pitch = (float) -Math.toDegrees(Math.atan2(next.y - p.y, VecUtil.flatDistance(next, p)));
				yaw = curve.angleStart() ;
				if(correctYaw) yaw += (float) rollAndOffsetInfo.getZOffsetSlopeStart(length);
			} else {
				Vec3d prev = points.get(i-1);
				Vec3d next = points.get(i+1);
				pitch = (float) -Math.toDegrees(Math.atan2(next.y - prev.y, VecUtil.flatDistance(next, prev)));
				yaw = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
			}

			float roll = 0;
			if(rolls.get(i) != 0) {
				if(!info.settings.rollAndOffsetInfo.degreeMode()) {
					double sin = rolls.get(i) * 0.01 * gaugeScale / gauge;//superelevision scale
					if(sin > 1) sin = 1;
					if(sin < -1) sin = -1;
					roll = (float) Math.toDegrees(Math.asin(sin));
				} else {
					roll = rolls.get(i).floatValue();
				}
			}

			res.add(new VecYPR(p.x, p.y, p.z, yaw, pitch, roll, -1));
		}

		cache.put(targetStepSize, Pair.of(stepSize, res));
		return cache.get(targetStepSize);
	}

	/* OVERRIDES */

	@Override
	public int costTies() {
		if (subBuilders == null) {
			return super.costTies();
		} else {
			return subBuilders.stream().mapToInt((BuilderBase::costTies)).sum();
		}
	}

	@Override
	public int costRails() {
		if (subBuilders == null) {
			return super.costRails();
		} else {
			return subBuilders.stream().mapToInt((BuilderBase::costRails)).sum();
		}
	}

	@Override
	public int costBed() {
		if (subBuilders == null) {
			return super.costBed();
		} else {
			return subBuilders.stream().mapToInt((BuilderBase::costBed)).sum();
		}
	}

	@Override
	public int costFill() {
		if (subBuilders == null) {
			return super.costFill();
		} else {
			return subBuilders.stream().mapToInt((BuilderBase::costFill)).sum();
		}
	}

	@Override
	public void setDrops(List<ItemStack> drops) {
		if (subBuilders == null) {
			super.setDrops(drops);
		} else {
			subBuilders.getFirst().setDrops(drops);
		}
	}


	@Override
	public boolean canBuild() {
		if (subBuilders == null) {
			return super.canBuild();
		} else {
			return subBuilders.stream().allMatch(BuilderBase::canBuild);
		}
	}

	@Override
	public void build() {
		if (subBuilders == null) {
			super.build();
		} else {
			subBuilders.forEach(BuilderBase::build);
		}
	}

	@Override
	public void clearArea() {
		if (subBuilders == null) {
			super.clearArea();
		} else {
			subBuilders.forEach(BuilderBase::clearArea);
		}
	}

	@Override
	public List<TrackBase> getTracksForRender() {
		if (subBuilders == null) {
			return super.getTracksForRender();
		} else {
			return subBuilders.subList(0, Math.min(subBuilders.size(), 3)).stream().map(BuilderBase::getTracksForRender).flatMap(List::stream).collect(Collectors.toList());
		}
	}

	@Override
	public List<TrackBase> getTracksForFloating() {
		if (subBuilders == null) {
			return super.getTracksForFloating();
		}
		return Collections.emptyList();
	}

	@Override
	public List<VecYPR> getRenderData() {
		if (subBuilders == null) {
			return super.getRenderData();
		} else {
			List<VecYPR> data = new ArrayList<>();
			for (BuilderBase curve : subBuilders.subList(0, Math.min(subBuilders.size(), 3))) {
				Vec3d offset = new Vec3d(curve.pos.subtract(pos));
				curve.getRenderData().stream().map(rd -> rd.add(offset)).forEach(data::add);
			}
			return data;
		}
	}
}