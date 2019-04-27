package cam72cam.mod.util;

import net.minecraft.util.EnumFacing;

public enum Axis {
    X(EnumFacing.Axis.X),
    Y(EnumFacing.Axis.Y),
    Z(EnumFacing.Axis.Z);

    public final EnumFacing.Axis internal;

    Axis(EnumFacing.Axis internal) {
        this.internal = internal;
    }

    public static Axis from(EnumFacing.Axis axis) {
        switch (axis) {
            case X:
                return X;
            case Y:
                return Y;
            case Z:
                return Z;
            default:
                return null;
        }
    }
}
