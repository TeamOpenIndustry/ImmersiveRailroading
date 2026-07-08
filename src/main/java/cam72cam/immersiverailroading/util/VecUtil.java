package cam72cam.immersiverailroading.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.FastMath;
import util.Matrix4;

public class VecUtil {
	private VecUtil() {
		// Disable construction since java does not have static classes
	}

	public static Vec3d fromYaw(double distance, float yaw) {
		return new Vec3d(Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
	public static float toYaw(Vec3d delta) {
		float yaw = (float) Math.toDegrees(FastMath.atan2(delta.x, delta.z));
		return (yaw + 360f) % 360f;
	}
	public static Vec3d rotateYaw(Vec3d pos, float rotationYaw) {
		if (rotationYaw - 90 == 0) {
			return pos;
		}
		//return new Matrix4().rotate(Math.toRadians(rotationYaw-90), 0, 1, 0).apply(pos);
		double cos = Math.cos(Math.toRadians(rotationYaw - 90));
		double sin = Math.sin(Math.toRadians(rotationYaw - 90));
		return new Vec3d(
				cos * pos.x + sin * pos.z,
				pos.y,
				-sin * pos.x + cos * pos.z
		);
	}
	public static Vec3d rotatePitch(Vec3d pos, float rotationPitch) {
		if (Math.abs(rotationPitch) < 0.01) {
			return pos;
		}
		// TODO optimize me!
		return new Matrix4().rotate(Math.toRadians(rotationPitch), 0, 0, 1).apply(pos);
	}

	public static Vec3d fromWrongYaw(double distance, float yaw)  {
		return new Vec3d(-Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
	
	public static float toWrongYaw(Vec3d delta) {
		float yaw = (float) Math.toDegrees(FastMath.atan2(-delta.x, delta.z));
		return (yaw + 360f) % 360f;
	}
	public static float toPitch(Vec3d delta) {
		float yaw = (float) Math.toDegrees(FastMath.atan2(Math.sqrt(delta.z * delta.z + delta.x * delta.x), delta.y));
		return (yaw + 360f) % 360f;
	}

	public static Vec3d rotateWrongYaw(Vec3d pos, float rotationYaw) {
		return fromWrongYaw(pos.x, rotationYaw).add(fromWrongYaw(pos.z, rotationYaw + 90).add(0, pos.y, 0));
	}

	public static Vec3d fromWrongYawPitch(float distance, float rotationYaw, float rotationPitch) {
		return fromWrongYaw(distance, rotationYaw).add(0, Math.tan(Math.toRadians(rotationPitch)) * distance, 0);
	}
	
	public static Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}
}
