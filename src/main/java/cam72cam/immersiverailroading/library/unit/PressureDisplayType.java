package cam72cam.immersiverailroading.library.unit;

public enum PressureDisplayType {
    psi,
    bar,
    kpa;

    public static final float psiToKPa = 6.89474f;
    public static final float kPaToPsi = 0.145037f;
    public static final float psiToBar = 0.0689474f;
    public static final float BarToPsi = 14.5037f;

    public float convertFromPSI(float value) {
        switch (this) {
            // 1 bar = 100 kPa
            case bar: return value * psiToKPa * 0.01f;
            case kpa: return value * psiToKPa;
            default: return value;
        }
    }

    public String toUnitString() {
        switch (this) {
            case bar:
                return "bar";
            case kpa:
                return "kPa";
            case psi:
            default:
                return "psi";
        }
    }
}
