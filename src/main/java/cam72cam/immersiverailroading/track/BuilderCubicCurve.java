package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderCubicCurve extends BuilderIterator {
	public List<BuilderCubicCurve> subBuilders;

	public BuilderCubicCurve(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}
	public BuilderCubicCurve(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos, endOfTrack);
		CubicCurve curve = getCurve();
		List<CubicCurve> subCurves = curve.subsplit(100);
		if (subCurves.size() > 1) {
			subBuilders = new ArrayList<>();
			for (CubicCurve subCurve : subCurves) {
				Vec3d delta = info.placementInfo.placementPosition;
				if (pos.equals(BlockPos.ORIGIN)) {
					delta = delta.subtract(new Vec3d(new BlockPos(info.placementInfo.placementPosition)));
				}
				PlacementInfo startPos = new PlacementInfo(subCurve.p1.add(delta), info.placementInfo.direction, subCurve.angleStart(), (float) subCurve.p1.distanceTo(subCurve.ctrl1));
				PlacementInfo endPos   = new PlacementInfo(subCurve.p2.add(delta), info.placementInfo.direction, subCurve.angleStop()+180, (float) subCurve.p2.distanceTo(subCurve.ctrl2));
				RailInfo subInfo = new RailInfo(info.world, info.settings.withType(TrackItems.CUSTOM), startPos, endPos, SwitchState.NONE, 0);
				BlockPos sPos = new BlockPos(startPos.placementPosition);
				BuilderCubicCurve subBuilder = new BuilderCubicCurve(subInfo, sPos);
				if (subBuilders.size() != 0) {
					for (TrackBase track : subBuilder.tracks) {
						if (track instanceof TrackRail) {
							track.overrideParent(subBuilders.get(0).getParentPos());
						}
					}
				} else {
					tracks = subBuilder.tracks;
				}
				subBuilders.add(subBuilder);
			}
		}
	}

	private HashMap<Double, List<PosStep>> cache;

	public CubicCurve getCurve() {
		Vec3d nextPos = VecUtil.fromYaw(info.settings.length, 45);

		if (info.customInfo != null && !info.customInfo.placementPosition.equals(info.placementInfo.placementPosition)) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition);
		}

		double magnitude1 = nextPos.lengthVector()/2;
		double magnitude2 = nextPos.lengthVector()/2;
		float angle = info.placementInfo.yaw;
		if (info.placementInfo.magnitude != 0) {
			magnitude1 = info.placementInfo.magnitude;
		}

		float angle2 = angle - 90;

		if (info.customInfo != null) {
			angle2 = info.customInfo.yaw;
			if (info.customInfo.magnitude != 0) {
				magnitude2 = info.customInfo.magnitude;
			}
		}

		Vec3d ctrl1 = VecUtil.fromYaw(magnitude1, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(magnitude2, angle2));

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

		List<Vec3d> points = curve.toList(stepSize);
		for(int i = 0; i < points.size(); i++) {
			Vec3d p = points.get(i);
			float angleCurve;
			if (i == points.size()-1) {
				angleCurve = curve.angleStop();
			} else if (i == 0) {
				angleCurve = curve.angleStart();
			} else {
				angleCurve = VecUtil.toYaw(points.get(i+1).subtract(points.get(i-1)));
			}
			res.add(new PosStep(p, angleCurve));
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
			return subBuilders.stream().mapToInt((BuilderCubicCurve::costTies)).sum();
		}
	}

	@Override
	public int costRails() {
		if (subBuilders == null) {
			return super.costRails();
		} else {
			return subBuilders.stream().mapToInt((BuilderCubicCurve::costRails)).sum();
		}
	}

	@Override
	public int costBed() {
		if (subBuilders == null) {
			return super.costBed();
		} else {
			return subBuilders.stream().mapToInt((BuilderCubicCurve::costBed)).sum();
		}
	}

	@Override
	public int costFill() {
		if (subBuilders == null) {
			return super.costFill();
		} else {
			return subBuilders.stream().mapToInt((BuilderCubicCurve::costFill)).sum();
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
			return subBuilders.stream().allMatch(BuilderCubicCurve::canBuild);
		}
	}

	@Override
	public void build() {
		if (subBuilders == null) {
			super.build();
		} else {
			subBuilders.stream().forEach(BuilderCubicCurve::build);
		}
	}

	@Override
	public void clearArea() {
		if (subBuilders == null) {
			super.clearArea();
		} else {
			subBuilders.stream().forEach(BuilderCubicCurve::clearArea);
		}
	}

	@Override
	public List<TrackBase> getTracksForRender() {
		if (subBuilders == null) {
			return super.getTracksForRender();
		} else {
			return subBuilders.subList(0, Math.min(subBuilders.size(), 3)).stream().map(BuilderCubicCurve::getTracksForRender).flatMap(List::stream).collect(Collectors.toList());
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		if (subBuilders == null) {
			return super.getRenderData();
		} else {
			List<VecYawPitch> data = new ArrayList<>();
			for (BuilderCubicCurve curve : subBuilders.subList(0, Math.min(subBuilders.size(), 3))) {
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