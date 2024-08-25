package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.data.TrackInfo;
import cam72cam.immersiverailroading.data.WorldData;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

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
		super(info, world, pos);
		CubicCurve curve = getCurve();
		List<CubicCurve> subCurves = curve.subsplit((int) (101 * 2 * 3.1415f / 4));
		if (subCurves.size() > 1) {
			subBuilders = new ArrayList<>();
			for (CubicCurve subCurve : subCurves) {
				// main pos -> subCurve's start pos
				Vec3d relOff = info.placementInfo.placementPosition.add(subCurve.p1);
				Vec3i relPos = new Vec3i(relOff);
				Vec3i sPos = pos.add(relPos);
				// The block remainder of curve position, with the subCurve move to origin block included
				Vec3d delta = relOff.subtract(relPos).subtract(subCurve.p1);
				//delta = delta.subtract(new Vec3i(delta)); // Relative position within the block
				PlacementInfo startPos = new PlacementInfo(subCurve.p1.add(delta), info.placementInfo.direction, subCurve.angleStart(), subCurve.ctrl1.add(delta));
				PlacementInfo endPos   = new PlacementInfo(subCurve.p2.add(delta), info.placementInfo.direction, subCurve.angleStop(), subCurve.ctrl2.add(delta));
				RailInfo subInfo = new RailInfo(info.settings.with(b -> b.type = TrackItems.CUSTOM), startPos, endPos, SwitchState.NONE, SwitchState.NONE, 0);

				BuilderCubicCurve subBuilder = new BuilderCubicCurve(subInfo, world, sPos);
				if (subBuilders.size() == 0) {
					// For render
					tracks = subBuilder.tracks;
				}
				subBuilders.add(subBuilder);
			}
		}
	}

	private HashMap<Double, List<PosStep>> cache;

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

		CubicCurve adjusted = new CubicCurve(Vec3d.ZERO, ctrl1, ctrl2, nextPos).linearize(info.settings.smoothing);
		ctrl1 = adjusted.ctrl1;
		ctrl2 = adjusted.ctrl2;

		if (info.placementInfo.control != null) {
			ctrl1= info.placementInfo.control.subtract(info.placementInfo.placementPosition);
		}
		if (info.customInfo.control != null && !isDefault) {
            ctrl2 = info.customInfo.control.subtract(info.placementInfo.placementPosition);
		}

		return new CubicCurve(Vec3d.ZERO, ctrl1, ctrl2, nextPos);
	}

	@Override
    public List<PosStep> getPath(double stepSize) {
		if (cache == null) {
			cache = new HashMap<>();
		}

		if (cache.containsKey(stepSize)) {
			return cache.get(stepSize);
		}

		List<PosStep> res = new ArrayList<>();
		CubicCurve curve = getCurve();

		// HACK for super long curves
		// Skip the super long calculation since it'll be overridden anyways
		curve = curve.subsplit(200).get(0);

		List<Vec3d> points = curve.toList(stepSize);
		for(int i = 0; i < points.size(); i++) {
			Vec3d p = points.get(i);
			float yaw;
			float pitch;
			if (points.size() == 1) {
				yaw = info.placementInfo.yaw;
				pitch = 0;
			} else if (i == points.size()-1) {
				Vec3d next = points.get(i-1);
				pitch = (float) Math.toDegrees(Math.atan2(next.y - p.y, next.distanceTo(p)));
                yaw = curve.angleStop();
			} else if (i == 0) {
				Vec3d next = points.get(i+1);
				pitch = (float) -Math.toDegrees(Math.atan2(next.y - p.y, next.distanceTo(p)));
				yaw = curve.angleStart();
			} else {
				Vec3d prev = points.get(i-1);
				Vec3d next = points.get(i+1);
				pitch = (float) -Math.toDegrees(Math.atan2(next.y - prev.y, next.distanceTo(prev)));
				yaw = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
			}
			res.add(new PosStep(p, yaw, pitch));
		}
		cache.put(stepSize, res);
		return cache.get(stepSize);
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
			subBuilders.get(0).setDrops(drops);
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
	public TrackInfo build(TrackInfo parent) {
		if (subBuilders == null) {
			return super.build(parent);
		} else {
			WorldData data = WorldData.get(world);
			TrackInfo info = data.allocateTrackInfo(this.info, parent);
			for (BuilderBase subBuilder : subBuilders) {
				subBuilder.build(info);
			}
			return info;
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
	public List<VecYawPitch> getRenderData() {
		if (subBuilders == null) {
			return super.getRenderData();
		} else {
			List<VecYawPitch> data = new ArrayList<>();
			for (BuilderBase curve : subBuilders.subList(0, Math.min(subBuilders.size(), 3))) {
				Vec3d offset = new Vec3d(curve.pos.subtract(pos));
				for (VecYawPitch rd : curve.getRenderData()) {
					rd = new VecYawPitch(rd.x + offset.x, rd.y + offset.y, rd.z + offset.z, rd.yaw, rd.pitch, rd.length);
					data.add(rd);
				}
			}
			return data;
		}
	}
}