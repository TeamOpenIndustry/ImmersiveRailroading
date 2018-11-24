package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BuilderTurnTable extends BuilderBase {
	protected HashSet<Pair<Integer, Integer>> positions;
	private BlockPos offset;

	public BuilderTurnTable(RailInfo info, BlockPos pos) {
		super(info.withLength(Math.min(info.settings.length, (int)(30 * info.settings.gauge.scale()))), pos);

		positions = new HashSet<Pair<Integer, Integer>>();
		
		offset = new BlockPos(0, 1, info.settings.length);
		
		double radius = info.settings.length;
		
		for (double irad = 1; irad <= radius + 1; irad++) {
			for (double angle = 0; angle < 360; angle+=0.5) {
				Vec3d gagPos = VecUtil.fromYaw(irad, (float) angle);
				positions.add(Pair.of((int)gagPos.x, (int)gagPos.z));
			}
		}
		
		this.setParentPos(new BlockPos(offset.down()));
		TrackRail main = new TrackRail(this, offset.down());
		tracks.add(main);
		
		for (Pair<Integer, Integer> pair : positions) {
			double toCenter = new Vec3d(pair.getLeft(), 0, pair.getRight()).lengthVector();
			
			if (toCenter > info.settings.length + 0.5) {
				continue;
			}
			
			if (!(pair.getLeft() == 0 && pair.getRight() == 0)) {
				TrackGag tgu = new TrackGag(this, new BlockPos(pair.getLeft() + offset.getX(), offset.getY()-1, pair.getRight() + offset.getZ()));
				if (toCenter > info.settings.length-0.5) {
					tgu.setHeight(1);
					tgu.setFlexible();
				}
				tracks.add(tgu);
			}
			
			TrackBase tg = new TrackGag(this, new BlockPos(pair.getLeft() + offset.getX(), offset.getY(), pair.getRight() + offset.getZ()));
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
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		for (float angle = 0; angle < 360; angle +=22.5) {
			Vec3d gagPos = VecUtil.rotateYaw(new Vec3d(0, 0, info.settings.length), angle-90);
			data.add(new VecYawPitch(gagPos.x + offset.getX(), gagPos.y + offset.getY(), gagPos.z + offset.getZ(), -angle));
		}
		
		float angle = 360/16.0f * (float)info.tablePos - info.placementInfo.facing.getHorizontalAngle();
		data.add(new VecYawPitch(offset.getX(), offset.getY(), offset.getZ(), -angle, 0, info.settings.length * 2, "RAIL_RIGHT", "RAIL_LEFT"));
		
		return data;
	}
	
	public int costTies() {
		return MathHelper.ceil((this.info.settings.length + 8) * ConfigBalance.TieCostMultiplier);
	}
	
	public int costRails() {
		return MathHelper.ceil((this.info.settings.length + 8)*2/3 * ConfigBalance.RailCostMultiplier);
	}
	
	public int costBed() {
		//TODO more accurate
		return MathHelper.ceil(this.tracks.size()/2.0 * 0.1 * ConfigBalance.BedCostMultiplier);
	}

	public int costFill() {
		int fillCount = 0;
		for (TrackBase track : tracks) {
			if (track.rel.getY() == 1) {
				continue;
			}
			if (BlockUtil.canBeReplaced(info.world, track.getPos().down(), false)) {
				fillCount += 1;
			}
		}
		return MathHelper.ceil(this.info.settings.railBedFill.getItem() != Items.AIR ? fillCount : 0);
	}
}