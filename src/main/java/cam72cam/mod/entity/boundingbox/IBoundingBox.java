package cam72cam.mod.entity.boundingbox;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.AxisAlignedBB;

public interface IBoundingBox {
    Vec3d min();
    Vec3d max();
    IBoundingBox expand(Vec3d val);
    IBoundingBox contract(Vec3d val);
    IBoundingBox grow(Vec3d val);
    IBoundingBox offset(Vec3d vec3d);
    double calculateXOffset(IBoundingBox other, double offsetX);
    double calculateYOffset(IBoundingBox other, double offsetY);
    double calculateZOffset(IBoundingBox other, double offsetZ);
    boolean intersects(Vec3d min, Vec3d max);
    boolean contains(Vec3d vec);

    static IBoundingBox from(AxisAlignedBB internal) {
        if (internal == null) {
            return null;
        }
        return new IBoundingBox() {
            @Override
            public Vec3d min() {
                return new Vec3d(internal.minX, internal.minY, internal.minZ);
            }

            @Override
            public Vec3d max() {
                return new Vec3d(internal.maxX, internal.maxY, internal.maxZ);
            }

            @Override
            public IBoundingBox expand(Vec3d centered) {
                return from(internal.expand(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox contract(Vec3d centered) {
                return from(internal.contract(centered.x, centered.y, centered.z));
            }

            @Override
            public IBoundingBox grow(Vec3d val) {
                return from(internal.grow(val.x, val.y, val.z));
            }

            @Override
            public IBoundingBox offset(Vec3d vec3d) {
                return from(internal.offset(vec3d.internal));
            }

            @Override
            public double calculateXOffset(IBoundingBox other, double offsetX) {
                return internal.calculateXOffset(new AxisAlignedBB(other.min().internal, other.max().internal), offsetX);
            }

            @Override
            public double calculateYOffset(IBoundingBox other, double offsetY) {
                return internal.calculateYOffset(new AxisAlignedBB(other.min().internal, other.max().internal), offsetY);
            }

            @Override
            public double calculateZOffset(IBoundingBox other, double offsetZ) {
                return internal.calculateZOffset(new AxisAlignedBB(other.min().internal, other.max().internal), offsetZ);
            }

            @Override
            public boolean intersects(Vec3d min, Vec3d max) {
                return internal.intersects(min.x, min.y, min.z, max.x, max.y, max.z);
            }

            @Override
            public boolean contains(Vec3d vec) {
                return internal.contains(vec.internal);
            }
        };
    }
    static IBoundingBox from(Vec3i pos) {
        return from(new AxisAlignedBB(pos.internal));
    }


    default boolean intersects(IBoundingBox bounds) {
        return this.intersects(bounds.min(), bounds.max());
    }
}
