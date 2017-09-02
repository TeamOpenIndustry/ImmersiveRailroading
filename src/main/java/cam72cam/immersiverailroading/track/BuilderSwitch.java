package cam72cam.immersiverailroading.track;

import java.util.List;

import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.util.math.BlockPos;

public class BuilderSwitch extends BuilderStraight {

	private BuilderTurn turnBuilder;

	public BuilderSwitch(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		RailInfo turnInfo = info.clone();
		turnInfo.type = TrackItems.TURN;

		turnBuilder = new BuilderTurn(turnInfo, pos);
		for(TrackBase track : turnBuilder.tracks) {
			if (track instanceof TrackRail) {
				track.overrideParent(new BlockPos(mainX, 0, mainZ));
			}
			for (TrackBase base : tracks) {
				if (base.rel_x == track.rel_x && base.rel_z == track.rel_z) {
					base.setFlexible();
				}
			}
		}
	}
	

	@Override
	public boolean canBuild() {
		return super.canBuild() && turnBuilder.canBuild();
	}
	
	@Override
	public void build() {
		super.build();
		turnBuilder.build();
	}
	
	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = super.getRenderData();
		data.addAll(turnBuilder.getRenderData());
		return data;
	}
}