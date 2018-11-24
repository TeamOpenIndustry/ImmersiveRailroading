package cam72cam.immersiverailroading.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class VecUtil {
	private VecUtil() {
		// Disable construction since java does not have static classes
	}
	
	public static Vec3d fromYaw(double distance, float yaw)  {
		return new Vec3d(-Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
	
	public static float toYaw(Vec3d delta) {
		float yaw = (float) Math.toDegrees(MathHelper.atan2(-delta.x, delta.z));
		return (yaw + 360f) % 360f;
	}
	public static float toPitch(Vec3d delta) {
		float yaw = (float) Math.toDegrees(MathHelper.atan2(MathHelper.sqrt(delta.z * delta.z + delta.x * delta.x), delta.y));
		return (yaw + 360f) % 360f;
	}

	public static Vec3d rotateYaw(Vec3d pos, float rotationYaw) {
		return fromYaw(pos.x, rotationYaw).add(fromYaw(pos.z, rotationYaw + 90).addVector(0, pos.y, 0));
	}

	public static Vec3d fromYawPitch(float distance, float rotationYaw, float rotationPitch) {
		return fromYaw(distance, rotationYaw).addVector(0, Math.tan(Math.toRadians(rotationPitch)) * distance, 0);
	}
	
	public static Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}
}
