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
		
		float slope = (1.0F/(info.length));
		float slopeAngle = (float) -Math.toDegrees(MathHelper.atan2(1, info.length));
		float actualLength = (float)new Vec3d(info.length, 1, 0).lengthVector();
		
		Vec3d pos = VecUtil.rotateYaw(new Vec3d(0, actualLength/2 * slope, actualLength/2.0-0.5), angle-90);
		data.add(new VecYawPitch(pos.x, pos.y, pos.z, -angle, slopeAngle, actualLength, "RAIL_RIGHT", "RAIL_LEFT"));
		
		double trackOffset = (1-info.gauge.scale())/4;
		
		for (double i = -trackOffset; i < info.length - trackOffset; i+=gauge.scale()) {
			pos = VecUtil.rotateYaw(new Vec3d(0, 0, i-0.25), angle-90);
			data.add(new VecYawPitch(pos.x, pos.y + slope * (i+gauge.scale()), pos.z, -angle, slopeAngle, "RAIL_BASE"));
		}
		return data;
	}
}
