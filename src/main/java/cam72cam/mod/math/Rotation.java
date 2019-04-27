package cam72cam.mod.math;

import cam72cam.mod.util.Facing;

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

    public static Rotation from(Facing facing) {
        switch (facing) {
            case NORTH:
                return Rotation.NONE;
            case EAST:
                return Rotation.CLOCKWISE_90;
            case SOUTH:
                return Rotation.CLOCKWISE_180;
            case WEST:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }
}
