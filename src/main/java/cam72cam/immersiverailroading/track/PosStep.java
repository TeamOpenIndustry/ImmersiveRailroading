package cam72cam.immersiverailroading.track;

import net.minecraft.util.math.Vec3d;

public class PosStep extends Vec3d {
    public final float yaw;

    public PosStep(double xIn, double yIn, double zIn, float yaw) {
        super(xIn, yIn, zIn);
        this.yaw = yaw;
    }

    public PosStep(Vec3d orig, float angle) {
        this(orig.x, orig.y, orig.z, angle);
    }
}
