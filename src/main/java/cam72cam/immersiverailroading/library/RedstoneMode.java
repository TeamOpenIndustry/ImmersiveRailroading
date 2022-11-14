package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum RedstoneMode {
    ENABLED,
    REQUIRED,
    INVERTED,
    DISABLED,
    COMPUTER
    ;

    @Override
    public String toString() {
        return "immersiverailroading:redstone_mode." + super.toString().toLowerCase(Locale.ROOT);
    }
}
