package cam72cam.mod.math;

public class Vec3d {
    protected final net.minecraft.util.math.Vec3d internal;
    public final double x;
    public final double y;
    public final double z;

    protected Vec3d(net.minecraft.util.math.Vec3d internal) {
        this.internal = internal;
        this.x = internal.x;
        this.y = internal.y;
        this.z = internal.z;
    }
    public Vec3d(double x, double y, double z) {
        this(new net.minecraft.util.math.Vec3d(x, y ,z));
    }
}
