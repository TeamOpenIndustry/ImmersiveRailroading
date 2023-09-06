package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum PressureDisplayType {
    psi,
    bar;

    public float convertFromPSI(float value) {
        return this == psi ? value : value * 0.0689476f;
    }

    public String toUnitString() {
        return toString().toUpperCase(Locale.ROOT);
    }
}
