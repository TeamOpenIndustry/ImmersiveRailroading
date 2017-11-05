package cam72cam.immersiverailroading.util;

import java.awt.Polygon;
import java.awt.geom.Rectangle2D;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/*
 * For now this just wraps the AABB constructor
 * 
 *  In the future we can override the intersects functions for better bounding boxes
 */
public class RealBB extends AxisAlignedBB {
	private double front;
	private double rear;
	private double width;
	private double height;
	private float yaw;
	private double centerX;
	private double centerY;
	private double centerZ;
	
	public RealBB(double front, double rear, double width, double height, float yaw) {
		this(front, rear, width, height, yaw, 0, 0, 0);		
	}
	
	private RealBB(double front, double rear, double width, double height, float yaw, double centerX, double centerY, double centerZ) {
		this(constructorParams(front, rear, width, height, yaw, centerX, centerY, centerZ));
		this.front = front;
		this.rear = rear;
		this.width = width;
		this.height = height;
		this.yaw = yaw;
		this.centerX = centerX;
		this.centerY = centerY;
		this.centerZ = centerZ;
	}
	
	private RealBB(double[] constructorParams) {
		super(constructorParams[0], constructorParams[1], constructorParams[2], constructorParams[3], constructorParams[4], constructorParams[5]);
	}
	
	private static AxisAlignedBB newBB(Vec3d min, Vec3d max) {
		//Why the fuck is this ClientOnly?
		return new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z);
	}

	private static double[] constructorParams(double front, double rear, double width, double height, float yaw, double centerX, double centerY, double centerZ) {
		Vec3d frontPos = VecUtil.fromYaw(front, yaw);
		Vec3d rearPos = VecUtil.fromYaw(rear, yaw);

		// width
		Vec3d offsetRight = VecUtil.fromYaw(width / 2, yaw + 90);
		Vec3d offsetLeft = VecUtil.fromYaw(width / 2, yaw - 90);
		AxisAlignedBB rightBox = newBB(frontPos.add(offsetRight), rearPos.add(offsetRight));
		AxisAlignedBB leftBox = newBB(frontPos.add(offsetLeft), rearPos.add(offsetLeft));

		AxisAlignedBB newthis = rightBox.union(leftBox).offset(centerX, centerY, centerZ);
		return new double[] { newthis.maxX, newthis.maxY + height, newthis.maxZ, newthis.minX, newthis.minY, newthis.minZ };
	}
	
	public RealBB clone() {
		return new RealBB(front, rear, width, height, yaw, centerX, centerY, centerZ);
	}
	public AxisAlignedBB setMaxY(double y2) {
		return this.clone();
	}
	public AxisAlignedBB contract(double x, double y, double z) {
		RealBB expanded = this.clone();
		
		if (x > 0) {
			expanded.front -= x;
		} else {
			expanded.rear -= x;
		}
		
		if (y > 0) {
			expanded.height -= y;
		} else {
			expanded.centerY -= y;
		}
		
		expanded.width -= z;
		
		return expanded.clone();
	}
	public AxisAlignedBB expand(double x, double y, double z) {
		RealBB expanded = this.clone();
		
		if (x > 0) {
			expanded.front += x;
		} else {
			expanded.rear += x;
		}
		
		if (y > 0) {
			expanded.height += y;
		} else {
			expanded.centerY += y;
		}
		
		expanded.width += z;
		
		return expanded.clone();
	}
	public AxisAlignedBB grow(double x, double y, double z) {
		RealBB growed = this.clone();
		growed.front += x;
		growed.rear += x;
		growed.height += y;
		growed.centerY += y;
		growed.width += z + z;
		return growed;
	}
	public AxisAlignedBB intersect(AxisAlignedBB p_191500_1_) {
		return this.clone();
	}
	public AxisAlignedBB union(AxisAlignedBB other) {
		return this.clone();
	}
	
	@Override
	public AxisAlignedBB offset(BlockPos pos) {
		return this.offset(pos.getX(), pos.getY(), pos.getZ());
	}
	@Override
	public AxisAlignedBB offset(Vec3d vec) {
		return this.offset(vec.x, vec.y, vec.z);
	}
	@Override
	public AxisAlignedBB offset(double x, double y, double z) {
		RealBB offsetted = this.clone();
		offsetted.centerX += x;
		offsetted.centerY += y;
		offsetted.centerZ += z;
		return offsetted.clone();
	}
	
	public double calculateXOffset(AxisAlignedBB other, double offsetX) {
		return 0;
	}
	public double calculateYOffset(AxisAlignedBB other, double offsetY) {
		return 0;
	}
	public double calculateZOffset(AxisAlignedBB other, double offsetZ) {
		return 0;
	}
	public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
		boolean doesIntersect = true;
		
		double actualYMin = this.centerY;
		double actualYMax = this.centerY + this.height;
		doesIntersect = doesIntersect && actualYMin < y2 && actualYMax > y1;
		
		Vec3d origin = VecUtil.rotateYaw(new Vec3d(this.rear, 0, -this.width/2), yaw).addVector(this.centerX, 0, this.centerZ);
		Vec3d point1 = VecUtil.fromYaw(front - rear, yaw).add(origin);
		Vec3d point2 = VecUtil.fromYaw(width, yaw + 90).add(origin); // might be +90
		Vec3d opposite = VecUtil.rotateYaw(new Vec3d(this.front, 0, this.width/2), yaw).addVector(this.centerX, 0, this.centerZ);
		
		
		// Scale by 100 
		int[] xp = new int[] { (int) (100 * origin.x), (int) (100 * point1.x), (int) (100 * opposite.x), (int) (100 * point2.x)};
		int[] zp = new int[] { (int) (100 * origin.z), (int) (100 * point1.z), (int) (100 * opposite.z), (int) (100 * point2.z)};
		
		Polygon rect = new Polygon(xp, zp, 4);
		
		if (x1 == x2 && y1 == y2 && z1 == z2) {
			// Single point
			doesIntersect = doesIntersect && rect.contains(x1*100, z1*100);	
		} else {
			// AABB
			Rectangle2D r = new Rectangle2D.Double(x1*100, z1*100, 0, 0);
			r.add(x2*100, z2*100);
			doesIntersect = doesIntersect && rect.intersects(r );
		}
		return doesIntersect;
	}
	public boolean contains(Vec3d vec) {
		return this.intersects(vec.x, vec.y, vec.z, vec.x, vec.y, vec.z);
	}
	public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB) {
		// This does NOT set enumfacing.  The places where this code (entity) is used don't use that value as of 1.12.
		int steps = 10;
		double xDist = vecB.x - vecA.x;
		double yDist = vecB.y - vecA.y;
		double zDist = vecB.z - vecA.z;
		double xDelta = xDist / steps;
		double yDelta = yDist / steps;
		double zDelta = zDist / steps;
		for (int step = 0; step < steps; step ++) {
			Vec3d stepPos = new Vec3d(vecA.x + xDelta * step, vecA.y + yDelta * step, vecA.z + zDelta * step);
			if (this.contains(stepPos)) {
				return new RayTraceResult(stepPos, EnumFacing.UP); 
			}
		}
		return null;
	}
}
