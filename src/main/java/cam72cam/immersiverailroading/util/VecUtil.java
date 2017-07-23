package cam72cam.immersiverailroading.util;

import net.minecraft.util.math.Vec3d;

public class VecUtil {
	private VecUtil() {
		// Disable construction since java does not have static classes
	}
	
	public static Vec3d fromYaw(double distance, float yaw)  {
		return new Vec3d(-Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
}
