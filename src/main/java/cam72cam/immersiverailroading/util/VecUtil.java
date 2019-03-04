package cam72cam.immersiverailroading.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import util.Matrix4;

public class VecUtil {
	private VecUtil() {
		// Disable construction since java does not have static classes
	}

	public static Vec3d fromYaw(double distance, float yaw) {
		return new Vec3d(Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
	public static float toYaw(Vec3d delta) {
		float yaw = (float) Math.toDegrees(MathHelper.atan2(delta.x, delta.z));
		return (yaw + 360f) % 360f;
	}
	public static Vec3d rotateYaw(Vec3d pos, float rotationYaw) {
		//return fromYaw(pos.x, rotationYaw).add(fromYaw(pos.z, rotationYaw + 90).addVector(0, pos.y, 0));
		return new Matrix4().rotate(Math.toRadians(rotationYaw-90), 0, 1, 0).apply(pos);
	}
	public static Vec3d rotatePitch(Vec3d pos, float rotationPitch) {
		return new Matrix4().rotate(Math.toRadians(rotationPitch), 0, 0, 1).apply(pos);
	}

	public static Vec3d fromWrongYaw(double distance, float yaw)  {
		return new Vec3d(-Math.sin(Math.toRadians(yaw)) * distance, 0, Math.cos(Math.toRadians(yaw)) * distance);
	}
	
	public static float toWrongYaw(Vec3d delta) {
		float yaw = (float) Math.toDegrees(MathHelper.atan2(-delta.x, delta.z));
		return (yaw + 360f) % 360f;
	}
	public static float toPitch(Vec3d delta) {
		float yaw = (float) Math.toDegrees(MathHelper.atan2(MathHelper.sqrt(delta.z * delta.z + delta.x * delta.x), delta.y));
		return (yaw + 360f) % 360f;
	}

	public static Vec3d rotateWrongYaw(Vec3d pos, float rotationYaw) {
		return fromWrongYaw(pos.x, rotationYaw).add(fromWrongYaw(pos.z, rotationYaw + 90).addVector(0, pos.y, 0));
	}

	public static Vec3d fromWrongYawPitch(float distance, float rotationYaw, float rotationPitch) {
		return fromWrongYaw(distance, rotationYaw).addVector(0, Math.tan(Math.toRadians(rotationPitch)) * distance, 0);
	}
	
	public static Vec3d between(Vec3d front, Vec3d rear) {
		return new Vec3d((front.x + rear.x) / 2, (front.y + rear.y) / 2, (front.z + rear.z) / 2);
	}

	public static Vec3d min(Vec3d a, Vec3d b) {
		if (a.x > b.x) {
			a = new Vec3d(b.x, a.y, a.z);
		}
		if (a.y > b.y) {
			a = new Vec3d(a.x, b.y, a.z);
		}
		if (a.z > b.z) {
			a = new Vec3d(a.x, a.y, b.z);
		}
		return a;
	}

	public static Vec3d max(Vec3d a, Vec3d b) {
		if (a.x < b.x) {
			a = new Vec3d(b.x, a.y, a.z);
		}
		if (a.y < b.y) {
			a = new Vec3d(a.x, b.y, a.z);
		}
		if (a.z < b.z) {
			a = new Vec3d(a.x, a.y, b.z);
		}
		return a;
	}

}
