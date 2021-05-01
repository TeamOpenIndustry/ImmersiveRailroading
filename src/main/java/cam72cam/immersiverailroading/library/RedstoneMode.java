package cam72cam.immersiverailroading.library;

public enum RedstoneMode {
    ENABLED,
    REQUIRED,
    INVERTED,
    DISABLED
    ;

    @Override
    public String toString() {
        return "immersiverailroading:redstone_mode." + super.toString().toLowerCase();
    }
}
