package cam72cam.immersiverailroading.track;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BuilderSlope extends BuilderStraight {
	public BuilderSlope(RailInfo info, BlockPos pos) {
		super(info, pos);
		
		float slope = (1.0F/(info.length + 1));
		
		for (TrackBase base : tracks) {
			base.setHeight((float) Math.min((new Vec3d(base.rel_x, 0, base.rel_z)).lengthVector() * slope, 0.9));
		}
	}

	@Override
	public List<VecYawPitch> getRenderData() {
		List<VecYawPitch> data = new ArrayList<VecYawPitch>();
		
		float slope = (1.0F/(info.length + 1));
		
		Vec3d pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, 0), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, (float) -Math.toDegrees(MathHelper.atan2(1, info.length)), info.length, "RAIL_RIGHT", "RAIL_LEFT"));
		
		for (int i = 0; i < info.length; i++) {
			pos = VecUtil.rotateYaw(new Vec3d(-0.5, 0, i), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y + slope * i, pos.z, -angle, (float) -Math.toDegrees(MathHelper.atan2(1, info.length)), "RAIL_BASE"));
		}
		return data;
	}
}
