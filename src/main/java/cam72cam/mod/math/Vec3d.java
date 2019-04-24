package cam72cam.mod.math;

public class Vec3d {
    public static final Vec3d ZERO = new Vec3d(net.minecraft.util.math.Vec3d.ZERO);
    public final net.minecraft.util.math.Vec3d internal;
    public final double x;
    public final double y;
    public final double z;

    public Vec3d(net.minecraft.util.math.Vec3d internal) {
        this.internal = internal;
        this.x = internal.x;
        this.y = internal.y;
        this.z = internal.z;
    }
    public Vec3d(double x, double y, double z) {
        this(new net.minecraft.util.math.Vec3d(x, y ,z));
    }

    public Vec3d(Vec3i pos) {
        this(pos.x, pos.y, pos.z);
    }

    public Vec3d add(double x, double y, double z) {
        return new Vec3d(internal.addVector(x, y, z));
    }
}
