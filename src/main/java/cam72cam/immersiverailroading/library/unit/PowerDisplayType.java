package cam72cam.immersiverailroading.library.unit;

public enum PowerDisplayType {
    kw,
    w,
    horsepower,
    ps;

    public static final float kWToHp = 1.34102f;
    public static final float hpToKW = 0.745701f;
    public static final float wToHp = 0.00134102f;
    public static final float PSToKW = 0.735498f;
    public static final float wToPS = 0.00135962f;

    public float convertFromWatt(float value) {
        switch (this) {
            default:
            case kw:
                return value / 1000f;
            case w:
                return value;
            case horsepower:
                return value * wToHp;
            case ps:
                return value * wToPS;
        }
    }

    public String toUnitString() {
        switch (this) {
            case w:
                return "W";
            case kw:
                return "kW";
            case ps:
                return "PS";
            case horsepower:
            default:
                return "hp";
        }
    }
}
