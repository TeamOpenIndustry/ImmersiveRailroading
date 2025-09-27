package cam72cam.immersiverailroading.library.unit;

public enum ForceDisplayType {
    n,
    kn,
    lbf,
    ;

    public static final float lbfToNewton = 4.448221f;

    public float convertFromNewton(float value) {
        switch (this) {
            case n:
            default:
                return value;
            case kn:
                return value / 1000f;
            case lbf:
                return value * 0.224809f;
        }
    }

    public String toUnitString() {
        switch (this) {
            case n:
            default:
                return "N";
            case kn:
                return "kN";
            case lbf:
                return "lbf";
        }
    }
}
