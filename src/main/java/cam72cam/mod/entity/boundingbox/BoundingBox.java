package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;

public class BoundingBox extends AxisAlignedBB {
    private final IBoundingBox internal;

    private static double[] hack(IBoundingBox internal) {
        Vec3d min = internal.min();
        Vec3d max = internal.max();
        return new double[] { max.x, max.y, max.z, min.x, min.y, min.z };
    }

    private BoundingBox(IBoundingBox internal, double[] constructorParams) {
        super(constructorParams[0], constructorParams[1], constructorParams[2], constructorParams[3], constructorParams[4], constructorParams[5]);
        this.internal = internal;
    }
    public BoundingBox(IBoundingBox internal) {
        this(internal, hack(internal));
    }

    /* NOP */
    @Override
    public BoundingBox setMaxY(double y) {
        // Used by blockwall
        return this;
    }
    @Override
    public BoundingBox intersect(AxisAlignedBB p_191500_1_) {
        // Used by piston
        return this;
    }
    @Override
    public BoundingBox union(AxisAlignedBB other) {
        // Used by piston
        // Used by entityliving for BB stuff
        return this;
    }

    /* Modifiers */

    @Override
    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(internal.expand(new Vec3d(x, y, z)));
    }
    @Override
    public BoundingBox contract(double x, double y, double z) {
        return new BoundingBox(internal.contract(new Vec3d(x, y, z)));
    }
    public BoundingBox grow(double x, double y, double z) {
        return new BoundingBox(internal.grow(new Vec3d(x, y, z)));
    }
    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(internal.offset(new Vec3d(x, y, z)));
    }

    /* Interactions */
    @Override
    public double calculateXOffset(AxisAlignedBB other, double offsetX) {
        return internal.calculateXOffset(IBoundingBox.from(other), offsetX);
    }
    @Override
    public double calculateYOffset(AxisAlignedBB other, double offsetY) {
        // TODO check if other is already a custom BB
        return internal.calculateYOffset(IBoundingBox.from(other), offsetY);
    }
    @Override
    public double calculateZOffset(AxisAlignedBB other, double offsetZ) {
        return internal.calculateZOffset(IBoundingBox.from(other), offsetZ);
    }
    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return internal.intersects(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
    }
    @Override
    public boolean contains(net.minecraft.util.math.Vec3d vec) {
        return internal.contains(new Vec3d(vec));
    }
    @Override
    public RayTraceResult calculateIntercept(net.minecraft.util.math.Vec3d vecA, net.minecraft.util.math.Vec3d vecB) {
        int steps = 10;
        double xDist = vecB.x - vecA.x;
        double yDist = vecB.y - vecA.y;
        double zDist = vecB.z - vecA.z;
        double xDelta = xDist / steps;
        double yDelta = yDist / steps;
        double zDelta = zDist / steps;
        for (int step = 0; step < steps; step ++) {
            Vec3d stepPos = new Vec3d(vecA.x + xDelta * step, vecA.y + yDelta * step, vecA.z + zDelta * step);
            if (internal.contains(stepPos)) {
                return new RayTraceResult(stepPos.internal, EnumFacing.UP);
            }
        }
        return null;
    }
}
