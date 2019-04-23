package cam72cam.mod.util;

import net.minecraft.util.EnumHand;

public enum Hand {
    PRIMARY(EnumHand.MAIN_HAND),
    SECONDARY(EnumHand.OFF_HAND),
    ;

    public final EnumHand internal;

    Hand(EnumHand internal) {
        this.internal = internal;
    }

    public static Hand from(EnumHand hand) {
        switch (hand) {
            case MAIN_HAND:
                return PRIMARY;
            case OFF_HAND:
                return SECONDARY;
            default:
                return null;
        }
    }
}
