package cam72cam.immersiverailroading.library.unit;

public enum PressureDisplayType {
    bar,
    kpa,
    psi;

    public static final float psiToKPa = 6.89474f;
    public static final float kPaToPsi = 0.145037f;
    public static final float psiToBar = 0.0689474f;
    public static final float BarToPsi = 14.5037f;

    public float convertFromPSI(float value) {
        switch (this) {
            // 1 bar = 100 kPa
            default:
            case bar:
                return value * psiToBar;
            case kpa:
                return value * psiToKPa;
            case psi:
                return value;
        }
    }

    public String toUnitString() {
        switch (this) {
            default:
            case bar:
                return "bar";
            case kpa:
                return "kPa";
            case psi:
                return "psi";
        }
    }
}
