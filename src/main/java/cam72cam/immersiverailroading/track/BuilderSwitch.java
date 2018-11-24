package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class BuilderSwitch extends BuilderBase implements IIterableTrack {

	private BuilderIterator turnBuilder;
	private BuilderStraight straightBuilder;

	public BuilderSwitch(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		RailInfo turnInfo = info.withType(info.customInfo.placementPosition.equals(info.placementInfo.placementPosition) ? TrackItems.TURN : TrackItems.CUSTOM);
		RailInfo straightInfo = info.clone();

		{
			turnBuilder = (BuilderIterator) turnInfo.getBuilder(pos);
			straightBuilder = new BuilderStraight(straightInfo, pos, true);
			
			double maxOverlap = 0;
			
			straightBuilder.positions.retainAll(turnBuilder.positions);
			
			for (Pair<Integer, Integer> straight : straightBuilder.positions) {
				maxOverlap = Math.max(maxOverlap, new Vec3d(straight.getKey(), 0, straight.getValue()).lengthVector());
			}
			
			maxOverlap *= 1.2;
			straightInfo = straightInfo.withLength((int) Math.ceil(maxOverlap) + 3);
		}
		

		straightBuilder = new BuilderStraight(straightInfo, pos, true);
		
		turnBuilder.overrideFlexible = true;
		
		for(TrackBase turn : turnBuilder.tracks) {
			if (turn instanceof TrackRail) {
				turn.overrideParent(straightBuilder.getParentPos());
			}
		}
		for (TrackBase straight : straightBuilder.tracks) {
			if (straight instanceof TrackGag) {
				straight.setFlexible();
			}
		}
	}
	
	@Override
	public int costTies() {
		return straightBuilder.costTies() + turnBuilder.costTies();
	}
	
	@Override
	public int costRails() {
		return straightBuilder.costRails() + turnBuilder.costRails();
	}

	@Override
	public int costBed() {
		return straightBuilder.costBed() + turnBuilder.costBed();
	}
	
	@Override
	public int costFill() {
		return straightBuilder.costFill() + turnBuilder.costFill();
	}
	
	@Override
	public void setDrops(List<ItemStack> drops) {
		straightBuilder.setDrops(drops);
	}
	

	@Override
	public boolean canBuild() {
		return straightBuilder.canBuild() && turnBuilder.canBuild();
	}
	
	@Override
	public void build() {
		straightBuilder.build();
		turnBuilder.build();
	}
	
	@Override
	public void clearArea() {
		straightBuilder.clearArea();
		turnBuilder.clearArea();
	}
	
	@Override
	public List<TrackBase> getTracksForRender() {
		List<TrackBase> data = straightBuilder.getTracksForRender();
		data.addAll(turnBuilder.getTracksForRender());
		return data;
	}
	
	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = straightBuilder.getRenderData();
		data.addAll(turnBuilder.getRenderData());
		return data;
	}

	public boolean isOnStraight(Vec3d position) {
		for (float dist = 0; dist < info.settings.length; dist += info.settings.gauge.scale()/8) {
			Vec3d gagPos = VecUtil.fromYaw(dist, straightBuilder.angle);
			gagPos = VecUtil.rotateYaw(gagPos, straightBuilder.info.placementInfo.facing.getHorizontalAngle() + 90 + 180);
			gagPos = gagPos.add(info.placementInfo.placementPosition);
			if (gagPos.distanceTo(position.addVector(0, -(position.y % 1), 0)) < info.settings.gauge.scale()/2) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<PosStep> getPath(double stepSize) {
		return straightBuilder.getPath(stepSize);
	}
}