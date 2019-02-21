package cam72cam.immersiverailroading.track;

import net.minecraft.util.math.Vec3d;

public class PosStep extends Vec3d {
    public final float yaw;
    public final float pitch;

    public PosStep(double xIn, double yIn, double zIn, float yaw, float pitch) {
        super(xIn, yIn, zIn);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public PosStep(Vec3d orig, float angle, float pitch) {
        this(orig.x, orig.y, orig.z, angle, pitch);
    }
}
