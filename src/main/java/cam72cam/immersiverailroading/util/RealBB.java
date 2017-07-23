package cam72cam.immersiverailroading.util;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/*
 * For now this just wraps the AABB constructor
 * 
 *  In the future we can override the intersects functions for better bounding boxes
 */
public class RealBB extends AxisAlignedBB {
	public RealBB(double front, double rear, double width, double height, float yaw) {
		// I hate java sometimes
		// super constructors must be the first call in a constructor, what sort
		// of shit is that
		super(c(front, rear, width, height, yaw)[0], c(front, rear, width, height, yaw)[1], c(front, rear, width, height, yaw)[2],
				c(front, rear, width, height, yaw)[3], c(front, rear, width, height, yaw)[4], c(front, rear, width, height, yaw)[5]);
	}

	private static double[] c(double front, double rear, double width, double height, float yaw) {
		Vec3d frontPos = VecUtil.fromYaw(front, yaw);
		Vec3d rearPos = VecUtil.fromYaw(rear, yaw + 180);

		// width
		Vec3d offsetRight = VecUtil.fromYaw(width / 2, yaw + 90);
		Vec3d offsetLeft = VecUtil.fromYaw(width / 2, yaw - 90);
		AxisAlignedBB rightBox = new AxisAlignedBB(frontPos.add(offsetRight), rearPos.add(offsetRight));
		AxisAlignedBB leftBox = new AxisAlignedBB(frontPos.add(offsetLeft), rearPos.add(offsetLeft));

		AxisAlignedBB newthis = rightBox.union(leftBox);
		return new double[] { newthis.maxX, height, newthis.maxZ, newthis.minX, newthis.minY, newthis.minZ };
	}
}
