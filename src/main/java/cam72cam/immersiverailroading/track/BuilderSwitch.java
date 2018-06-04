package cam72cam.immersiverailroading.track;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderSwitch extends BuilderBase {

	private BuilderTurn turnBuilder;
	private BuilderStraight straightBuilder;

	public BuilderSwitch(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		//info.quarter = 0;
		
		RailInfo turnInfo = info.clone();
		RailInfo straightInfo = info.clone();
		turnInfo.type = TrackItems.TURN;

		{
			turnInfo.length += 1;
			turnBuilder = new BuilderTurn(turnInfo.clone(), pos);
			straightBuilder = new BuilderStraight(straightInfo.clone(), pos, true);
			
			double maxOverlap = 0;
			
			straightBuilder.positions.retainAll(turnBuilder.positions);
			
			for (Pair<Integer, Integer> straight : straightBuilder.positions) {
				maxOverlap = Math.max(maxOverlap, new Vec3d(straight.getKey(), 0, straight.getValue()).lengthVector());
			}
			
			maxOverlap *= 1.2;
			straightInfo.length = (int) Math.ceil(maxOverlap) + 1;
		}
		

		turnBuilder = new BuilderTurn(turnInfo, pos);
		straightBuilder = new BuilderStraight(straightInfo, pos, true);
		
		turnBuilder.overrideFlexible = true;
		
		for(TrackBase turn : turnBuilder.tracks) {
			if (turn instanceof TrackRail) {
				turn.overrideParent(new BlockPos(straightBuilder.mainX, 0, straightBuilder.mainZ));
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
		for (float dist = 0; dist < info.length; dist += gauge.scale()/8) {
			Vec3d gagPos = VecUtil.fromYaw(dist, straightBuilder.angle);
			gagPos = VecUtil.rotateYaw(gagPos, straightBuilder.info.facing.getHorizontalAngle() + 90 + 180);
			gagPos = gagPos.add(info.placementPosition);
			if (gagPos.distanceTo(position) < gauge.scale()/4) {
				return true;
			}
		}
		return false;
	}
}