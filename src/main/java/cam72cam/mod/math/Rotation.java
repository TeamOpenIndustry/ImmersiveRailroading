package cam72cam.mod.math;

public enum Rotation {
    NONE(net.minecraft.util.Rotation.NONE),
    CLOCKWISE_90(net.minecraft.util.Rotation.CLOCKWISE_90),
    CLOCKWISE_180(net.minecraft.util.Rotation.CLOCKWISE_180),
    COUNTERCLOCKWISE_90(net.minecraft.util.Rotation.COUNTERCLOCKWISE_90);
    public final net.minecraft.util.Rotation internal;

    Rotation(net.minecraft.util.Rotation internal) {
        this.internal = internal;
    }

    public static Rotation from(net.minecraft.util.Rotation rot) {
        switch (rot) {
            case NONE: return NONE;
            case CLOCKWISE_90: return CLOCKWISE_90;
            case CLOCKWISE_180: return CLOCKWISE_180;
            case COUNTERCLOCKWISE_90: return COUNTERCLOCKWISE_90;
            default: return null;
        }
    }
}
