package cam72cam.immersiverailroading.library.unit;

public enum PowerDisplayType {
    horsepower,
    w,
    kw,
    ;

    public static final float kwToHp = 1.359621f;
    public static final float hpToKW = 0.735499f;
    public static final float wToHp = 0.00135962f;

    public float convertFromWatt(float value) {
        switch (this) {
            case w:
                return value;
            case kw:
                return value / 1000f;
            case horsepower:
            default:
                return value * wToHp;
        }
    }

    public String toUnitString() {
        switch (this) {
            case w:
                return "W";
            case kw:
                return "kW";
            case horsepower:
            default:
                return "hp";
        }
    }
}
