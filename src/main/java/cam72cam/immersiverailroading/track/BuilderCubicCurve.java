package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BuilderCubicCurve extends BuilderIterator {
	private List<BuilderBase> subBuilders;

	public BuilderCubicCurve(RailInfo info, BlockPos pos) {
		this(info, pos, false);
	}

	@Override
	public List<BuilderBase> getSubBuilders() {
		return subBuilders;
	}

	public BuilderCubicCurve(RailInfo info, BlockPos pos, boolean endOfTrack) {
		super(info, pos, endOfTrack);
		CubicCurve curve = getCurve();
		List<CubicCurve> subCurves = curve.subsplit((int) (101 * 2 * 3.1415f / 4));
		if (subCurves.size() > 1) {
			subBuilders = new ArrayList<>();
			for (CubicCurve subCurve : subCurves) {
				Vec3d delta = info.placementInfo.placementPosition;
				if (pos.equals(BlockPos.ORIGIN)) {
					delta = delta.subtract(new Vec3d(new BlockPos(info.placementInfo.placementPosition)));
				}
				PlacementInfo startPos = new PlacementInfo(subCurve.p1.add(delta), info.placementInfo.direction, subCurve.angleStart(), subCurve.ctrl1.add(delta));
				PlacementInfo endPos   = new PlacementInfo(subCurve.p2.add(delta), info.placementInfo.direction, subCurve.angleStop()+180, subCurve.ctrl2.add(delta));
				RailInfo subInfo = new RailInfo(info.world, info.settings.withType(TrackItems.CUSTOM), startPos, endPos, SwitchState.NONE, SwitchState.NONE, 0);
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
		Vec3d nextPos = new Vec3d(new BlockPos(VecUtil.fromYaw(info.settings.length, info.placementInfo.yaw + 45)));

		boolean isDefault = info.customInfo.placementPosition.equals(info.placementInfo.placementPosition);
		if (!isDefault) {
			nextPos = info.customInfo.placementPosition.subtract(info.placementInfo.placementPosition);
		}

		double ctrlGuess = nextPos.lengthVector()/2;
		float angle = info.placementInfo.yaw;

		float angle2 = angle + 180;

		if (!isDefault) {
			angle2 = info.customInfo.yaw;
		}

		Vec3d ctrl1 = VecUtil.fromYaw(ctrlGuess, angle);
		Vec3d ctrl2 = nextPos.add(VecUtil.fromYaw(ctrlGuess, angle2));

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
	public void build() {
		if (subBuilders == null) {
			super.build();
		} else {
			subBuilders.stream().forEach(BuilderBase::build);
		}
	}

	@Override
	public void clearArea() {
		if (subBuilders == null) {
			super.clearArea();
		} else {
			subBuilders.stream().forEach(BuilderBase::clearArea);
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