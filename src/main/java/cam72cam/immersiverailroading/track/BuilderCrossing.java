package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BuilderCrossing extends BuilderBase {
	/*
	 * This is terrible
	 */

	public BuilderCrossing(RailInfo info, BlockPos pos) {
		super(info.withLength(3), pos);

		this.setParentPos(BlockPos.ORIGIN);
		
		tracks.add(new TrackRail(this, BlockPos.ORIGIN));
		int blocks = 1;
		for (int i = -blocks; i <=blocks; i ++) {
			for (int j = -blocks; j <=blocks; j ++) {
				if (i == 0 && j == 0) {
					continue;
				}
				tracks.add(new TrackGag(this, new BlockPos(i, 0, j)));
			}
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		float angle = 0;

		double trackOffset = (1-info.settings.gauge.scale())/4;
		
		Vec3d pos = VecUtil.rotateYaw(new Vec3d(0, 0, 0), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, 0, info.settings.length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (double i = -trackOffset; i < info.settings.length - trackOffset; i+=info.settings.gauge.scale()) {
			pos = VecUtil.rotateYaw(new Vec3d(0, 0, i-0.25), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y, pos.z-1, -angle, "RAIL_BASE"));
		}
		angle -= 90;
		
		pos = VecUtil.rotateYaw(new Vec3d(0, 0, 0), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, 0, info.settings.length, "RAIL_RIGHT", "RAIL_LEFT"));
		for (double i = -trackOffset; i < info.settings.length - trackOffset; i+=info.settings.gauge.scale()) {
			pos = VecUtil.rotateYaw(new Vec3d(0, 0, i-0.25), angle-90);
			data.add(new VecYawPitch(pos.x-1, pos.y, pos.z, -angle, "RAIL_BASE"));
		}
		
		return data;
	}

}
