package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;

public class BuilderTurnTable extends BuilderBase {
	protected HashSet<Pair<Integer, Integer>> positions;
	private Vec3i offset;

	public BuilderTurnTable(RailInfo info, World world, Vec3i pos) {
		super(info.withSettings(b -> b.length = Math.min(info.settings.length, (int)(30 * info.settings.gauge.scale()))), world, pos);

		positions = new HashSet<>();
		
		offset = new Vec3i(0, 1, info.settings.length);
		offset = offset.rotate(Rotation.from(info.placementInfo.facing()));
		
		double radius = info.settings.length;
		
		for (double irad = 1; irad <= radius + 1; irad++) {
			for (double angle = 0; angle < 360; angle+=0.5) {
				Vec3d gagPos = VecUtil.fromYaw(irad, (float) angle);
				positions.add(Pair.of((int)gagPos.x, (int)gagPos.z));
			}
		}
		
		this.setParentPos(offset.down());
		TrackRail main = new TrackRail(this, offset.down());
		tracks.add(main);
		
		for (Pair<Integer, Integer> pair : positions) {
			double toCenter = new Vec3d(pair.getLeft(), 0, pair.getRight()).length();
			
			if (toCenter > info.settings.length + 0.5) {
				continue;
			}
			
			if (!(pair.getLeft() == 0 && pair.getRight() == 0)) {
				TrackGag tgu = new TrackGag(this, new Vec3i(pair.getLeft() + offset.x, offset.y-1, pair.getRight() + offset.z));
				if (toCenter > info.settings.length-0.5) {
					tgu.setHeight(1);
					tgu.setFlexible();
				}
				tracks.add(tgu);
			}
			
			TrackBase tg = new TrackGag(this, new Vec3i(pair.getLeft() + offset.x, offset.y, pair.getRight() + offset.z));
			tg.setHeight(0.000001f);
			tg.solidNotRequired = true;
			if (toCenter > info.settings.length-0.5) {
				tg.setHeight(0);
			}
			if (toCenter > info.settings.length-1.5) {
				tg.setFlexible();
			}
			tracks.add(tg);
		}
	}
	
	@Override
	public List<TrackBase> getTracksForRender() {
		return this.tracks;
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<>();

		if (info.itemHeld) {
			for (float angle = 0; angle < 360; angle += (90f / PlacementInfo.segmentation())) {
				Vec3d gagPos = VecUtil.rotateWrongYaw(new Vec3d(0, 0, info.settings.length), angle - 90);
				data.add(new VecYawPitch(gagPos.x + offset.x, gagPos.y + offset.y, gagPos.z + offset.z, -angle));
			}
		}

		float angle = (float)info.tablePos - info.placementInfo.facing().getAngle();
		data.add(new VecYawPitch(offset.x, offset.y, offset.z, -angle, 0, info.settings.length * 2, "RAIL_RIGHT", "RAIL_LEFT"));
		
		return data;
	}
	
	public int costTies() {
		return (int)Math.ceil((this.info.settings.length + 8) * ConfigBalance.TieCostMultiplier);
	}
	
	public int costRails() {
		return (int)Math.ceil((this.info.settings.length + 8)*2/3 * ConfigBalance.RailCostMultiplier);
	}
	
	public int costBed() {
		//TODO more accurate
		return (int)Math.ceil(this.tracks.size()/2.0 * 0.1 * ConfigBalance.BedCostMultiplier);
	}

	public int costFill() {
		int fillCount = 0;
		for (TrackBase track : tracks) {
			if (track.rel.y == 1) {
				continue;
			}
			if (BlockUtil.canBeReplaced(world, track.getPos().down(), false)) {
				fillCount += 1;
			}
		}
		return (int)Math.ceil(!this.info.settings.railBedFill.isEmpty() ? fillCount : 0);
	}
}