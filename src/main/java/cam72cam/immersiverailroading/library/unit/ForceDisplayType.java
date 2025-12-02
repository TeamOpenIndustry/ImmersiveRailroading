package cam72cam.immersiverailroading.library.unit;

public enum ForceDisplayType {
    kn,
    n,
    lbf;

    public static final float lbfToNewton = 4.448221f;

    public float convertFromNewton(float value) {
        switch (this) {
            default:
            case kn:
                return value / 1000f;
            case n:
                return value;
            case lbf:
                return value * 0.224809f;
        }
    }

    public String toUnitString() {
        switch (this) {
            default:
            case kn:
                return "kN";
            case n:
                return "N";
            case lbf:
                return "lbf";
        }
    }
}
